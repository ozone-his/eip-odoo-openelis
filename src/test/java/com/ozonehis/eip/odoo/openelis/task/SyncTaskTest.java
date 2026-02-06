package com.ozonehis.eip.odoo.openelis.task;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ozonehis.eip.odoo.openelis.LocalDateTimeUtils;
import com.ozonehis.eip.odoo.openelis.SyncUtils;
import com.ozonehis.eip.odoo.openelis.fhir.OdooFhirClient;
import com.ozonehis.eip.odoo.openelis.fhir.OpenElisFhirClient;
import java.time.LocalDateTime;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class SyncTaskTest {

    private static final long OVERLAP = 15000;

    private MockedStatic<LocalDateTimeUtils> mockDateTimeUtils;

    private MockedStatic<SyncUtils> mockSyncUtils;

    @Mock
    private OpenElisFhirClient mockOpenElisClient;

    @Mock
    private OdooFhirClient mockOdooClient;

    @Mock
    private TimestampStore mockTimestampStore;

    private SyncTask task;

    @BeforeEach
    public void setUp() {
        mockDateTimeUtils = Mockito.mockStatic(LocalDateTimeUtils.class);
        mockSyncUtils = Mockito.mockStatic(SyncUtils.class);
        task = new SyncTask(mockTimestampStore, mockOpenElisClient, mockOdooClient);
        Whitebox.setInternalState(task, "overlap", OVERLAP);
    }

    @AfterEach
    public void tearDown() {
        mockDateTimeUtils.close();
        mockSyncUtils.close();
    }

    @Test
    public void sync_shouldFetchAndSyncAllTheModifiedResources() {
        LocalDateTime now = LocalDateTime.now();
        Patient p1 = new Patient();
        Patient p2 = new Patient();
        LocalDateTime lastSyncTs = now.minusMinutes(5);
        when(LocalDateTimeUtils.getCurrentTime()).thenReturn(now);
        when(mockTimestampStore.getTimestamp(Patient.class)).thenReturn(lastSyncTs);
        LocalDateTime effectiveLastSyncTs = lastSyncTs.minus(OVERLAP, MILLIS);
        when(mockOpenElisClient.getModifiedResources(Patient.class, effectiveLastSyncTs))
                .thenReturn(List.of(p1, p2));

        task.sync(Patient.class);

        verify(mockOdooClient).update(p1);
        verify(mockOdooClient).update(p2);
        verify(mockTimestampStore).update(now, Patient.class);
        mockSyncUtils.verify(() -> SyncUtils.clearLastUpdatedTimestamps());
    }

    @Test
    public void sync_shouldSkipAResourceThatIsUpToDateBasedOnTheInMemoryTracker() {
        LocalDateTime now = LocalDateTime.now();
        Patient p1 = new Patient();
        Patient p2 = new Patient();
        LocalDateTime lastSyncTs = now.minusMinutes(5);
        when(LocalDateTimeUtils.getCurrentTime()).thenReturn(now);
        when(mockTimestampStore.getTimestamp(Patient.class)).thenReturn(lastSyncTs);
        LocalDateTime effectiveLastSyncTs = lastSyncTs.minus(OVERLAP, MILLIS);
        when(mockOpenElisClient.getModifiedResources(Patient.class, effectiveLastSyncTs))
                .thenReturn(List.of(p1, p2));
        when(SyncUtils.skip(p1)).thenReturn(true);

        task.sync(Patient.class);

        verify(mockOdooClient, never()).update(p1);
        verify(mockOdooClient).update(p2);
        verify(mockTimestampStore).update(now, Patient.class);
        mockSyncUtils.verify(() -> SyncUtils.clearLastUpdatedTimestamps());
    }

    @Test
    public void sync_shouldUseTheEpochAsTheLastSyncTimestampIfNoTimestampIsStored() {
        LocalDateTime now = LocalDateTime.now();
        Patient p1 = new Patient();
        Patient p2 = new Patient();
        when(LocalDateTimeUtils.getCurrentTime()).thenReturn(now);
        LocalDateTime lastSyncTs = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        when(mockOpenElisClient.getModifiedResources(Patient.class, lastSyncTs)).thenReturn(List.of(p1, p2));

        task.sync(Patient.class);

        verify(mockOdooClient).update(p1);
        verify(mockOdooClient).update(p2);
        verify(mockTimestampStore).update(now, Patient.class);
        mockSyncUtils.verify(() -> SyncUtils.clearLastUpdatedTimestamps());
    }

    @Test
    public void execute_callsSyncForPatientsAndServiceRequests() {
        task = Mockito.spy(task);
        Mockito.doNothing().when(task).sync(Patient.class);
        Mockito.doNothing().when(task).sync(ServiceRequest.class);

        task.execute();

        verify(task).sync(Patient.class);
        verify(task).sync(ServiceRequest.class);
    }
}
