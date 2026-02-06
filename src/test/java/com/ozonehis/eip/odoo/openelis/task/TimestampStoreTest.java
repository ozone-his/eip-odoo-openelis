package com.ozonehis.eip.odoo.openelis.task;

import com.ozonehis.eip.odoo.openelis.EipFileUtils;
import com.ozonehis.eip.odoo.openelis.PropertiesUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Properties;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

@ExtendWith(MockitoExtension.class)
public class TimestampStoreTest {

    protected static String TZ_OFFSET = ZonedDateTime.now().getOffset().toString();

    private static MockedStatic<EipFileUtils> mockEipFileUtils;

    private static MockedStatic<PropertiesUtils> mockPropertiesUtils;

    @Mock
    private File mockFile;

    @Mock
    private Properties mockProperties;

    @Mock
    private FileOutputStream mockFileOutputStream;

    private TimestampStore store;

    @BeforeEach
    public void setUp() {
        mockEipFileUtils = Mockito.mockStatic(EipFileUtils.class);
        mockPropertiesUtils = Mockito.mockStatic(PropertiesUtils.class);
        store = new TimestampStore();
    }

    @AfterEach
    public void tearDown() {
        mockEipFileUtils.close();
        mockPropertiesUtils.close();
    }

    @Test
    public void getTimestamp_shouldReturnNullWhenGetTimestampIsCalledAndNoTimestampFound() {
        Whitebox.setInternalState(store, "props", new Properties());

        LocalDateTime result = store.getTimestamp(Patient.class);

        Assertions.assertNull(result);
    }

    @Test
    public void getTimestamp_shouldReturnTimestampWhenGetTimestampIsCalledAndTimestampFound() {
        final String ts = "2021-01-01T00:00:00.000" + TZ_OFFSET;
        LocalDateTime expected = ZonedDateTime.parse(ts).toLocalDateTime();
        Properties props = new Properties();
        props.put(Patient.class.getSimpleName(), ts);
        Whitebox.setInternalState(store, "props", props);

        LocalDateTime result = store.getTimestamp(Patient.class);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void update_shouldUpdateWhenLastSyncTimestampForTheResourceType() throws Exception {
        LocalDateTime oldPatientTs =
                ZonedDateTime.parse("2021-01-01T01:00:00.000" + TZ_OFFSET).toLocalDateTime();
        final String newPatientTsStr = "2021-01-01T01:00:00.000" + TZ_OFFSET;
        LocalDateTime newPatientTs = ZonedDateTime.parse(newPatientTsStr).toLocalDateTime();
        LocalDateTime serviceReqTs =
                ZonedDateTime.parse("2021-01-01T03:00:00.000" + TZ_OFFSET).toLocalDateTime();
        Properties existingProps = new Properties();
        existingProps.put(Patient.class.getSimpleName(), oldPatientTs);
        existingProps.put(ServiceRequest.class.getSimpleName(), serviceReqTs);
        Whitebox.setInternalState(store, "props", existingProps);
        Whitebox.setInternalState(store, "file", mockFile);
        Mockito.when(PropertiesUtils.createProperties()).thenReturn(mockProperties);
        Mockito.when(EipFileUtils.openOutputStream(mockFile)).thenReturn(mockFileOutputStream);

        store.update(newPatientTs, Patient.class);

        Mockito.verify(mockProperties).putAll(existingProps);
        Mockito.verify(mockProperties).put(Patient.class.getSimpleName(), newPatientTsStr);
        Mockito.verify(mockProperties).store(mockFileOutputStream, null);
        Assertions.assertEquals(newPatientTs, store.getTimestamp(Patient.class));
    }

    @Test
    public void update_shouldFailIfTheTimestampsAreNotSavedToFile() throws Exception {
        LocalDateTime newPatientTs =
                ZonedDateTime.parse("2021-01-01T01:00:00.000" + TZ_OFFSET).toLocalDateTime();
        Whitebox.setInternalState(store, "props", new Properties());
        Whitebox.setInternalState(store, "file", mockFile);
        Mockito.when(PropertiesUtils.createProperties()).thenReturn(mockProperties);
        Mockito.when(EipFileUtils.openOutputStream(mockFile)).thenReturn(mockFileOutputStream);
        Mockito.doThrow(new IOException()).when(mockProperties).store(mockFileOutputStream, null);

        RuntimeException e =
                Assertions.assertThrows(RuntimeException.class, () -> store.update(newPatientTs, Patient.class));

        Assertions.assertEquals(
                "Failed to save timestamps for " + Patient.class.getSimpleName() + " resource ", e.getMessage());
    }

    @Test
    public void getFile_shouldUseExistingFileWhenFileAlreadyExists() throws IOException {
        final String fileName = "test_timestamp.txt";
        Whitebox.setInternalState(store, "filename", fileName);
        Mockito.when(mockFile.exists()).thenReturn(true);
        Mockito.when(EipFileUtils.createFile(fileName)).thenReturn(mockFile);

        File result = store.getFile();

        Assertions.assertEquals(mockFile, result);
        Mockito.verify(mockFile, Mockito.never()).getParentFile();
        Mockito.verify(mockFile, Mockito.never()).createNewFile();
    }

    @Test
    public void getFile_shouldCreateNewFileWhenFileDoesNotExist() throws IOException {
        final String fileName = "test_timestamp.txt";
        Whitebox.setInternalState(store, "filename", fileName);
        File mockDir = Mockito.mock(File.class);
        Mockito.when(mockFile.getParentFile()).thenReturn(mockDir);
        Mockito.when(mockDir.exists()).thenReturn(true);
        Mockito.when(mockFile.createNewFile()).thenReturn(true);
        Mockito.when(EipFileUtils.createFile(fileName)).thenReturn(mockFile);

        File result = store.getFile();

        Assertions.assertEquals(mockFile, result);
        Mockito.verify(mockFile).createNewFile();
        Mockito.verify(mockDir, Mockito.never()).mkdirs();
    }

    @Test
    public void getFile_shouldCreateParentDirectoriesIfTheyDoNotExist() throws IOException {
        final String fileName = "test_timestamp.txt";
        Whitebox.setInternalState(store, "filename", fileName);
        File mockDir = Mockito.mock(File.class);
        Mockito.when(mockFile.getParentFile()).thenReturn(mockDir);
        Mockito.when(mockDir.mkdirs()).thenReturn(true);
        Mockito.when(mockFile.createNewFile()).thenReturn(true);
        Mockito.when(EipFileUtils.createFile(fileName)).thenReturn(mockFile);

        File result = store.getFile();

        Assertions.assertEquals(mockFile, result);
        Mockito.verify(mockFile).createNewFile();
        Mockito.verify(mockDir).mkdirs();
    }
}
