/**
 * Copyright (C) 2009-2012 enStratus Networks Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.encryption.Encryption;
import org.dasein.cloud.encryption.EncryptionException;
import org.dasein.cloud.storage.CloudStoreObject;
import org.dasein.cloud.storage.FileTransfer;
import org.dasein.util.CalendarWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BlobStoreTestCase extends BaseTestCase {
    static public class StorageEncryption implements Encryption {
        byte[] encryptionKey;
        
        public StorageEncryption() {
            encryptionKey = "12345678901234567890123456789012".getBytes();
        }
        
        public void clear() {
            for( int i=0; i<encryptionKey.length; i++ ) {
                encryptionKey[i] = '\0';
            }
        }

        public void decrypt(InputStream input, OutputStream output) throws EncryptionException {
            try {
                SecretKeySpec spec = new SecretKeySpec(encryptionKey, "AES");
                Cipher cipher = Cipher.getInstance("AES");
                byte[] buf = new byte[1024];
                int count = 0;
                
                cipher.init(Cipher.DECRYPT_MODE, spec);
                input = new CipherInputStream(input, cipher);
                while( (count = input.read(buf)) >= 0) {
                    output.write(buf, 0, count);
                }
                output.close();
            }
            catch( GeneralSecurityException e ) {
                throw new EncryptionException(e);
            }
            catch( IOException e ) {
                throw new EncryptionException(e);
            }                
        }

        public void encrypt(InputStream input, OutputStream output) throws EncryptionException {
            try {
                SecretKeySpec spec = new SecretKeySpec(encryptionKey, "AES");
                Cipher cipher = Cipher.getInstance("AES");
                byte[] buf = new byte[1024];
                int count;
                
                cipher.init(Cipher.ENCRYPT_MODE, spec);
                output = new CipherOutputStream(output, cipher);
                while( (count = input.read(buf)) >= 0 ) {
                    output.write(buf, 0, count);
                }
                output.close();                
            }
            catch( GeneralSecurityException e ) {
                throw new EncryptionException(e);
            }
            catch( IOException e ) {
                throw new EncryptionException(e);
            }
        }
    };
    
    private CloudProvider cloud             = null;
    private String        directoryToRemove = null;
    private String        testDirectory     = null;
    private String        testFileName      = null;
    private File          testLocalFile     = null;
    
    public BlobStoreTestCase(String name) { super(name); }

    private void download(String directory, String object, boolean multipart, Encryption encryption) throws Exception {
        File targetFile = new File("dsn" + System.currentTimeMillis() + ".txt");
            
        if( targetFile.exists() ) {
            targetFile.delete();
        }
        try {
            FileTransfer task;

            if( !multipart ) {
                CloudStoreObject cloudObject = new org.dasein.cloud.storage.CloudStoreObject();
                    
                cloudObject.setContainer(false);
                cloudObject.setDirectory(directory);
                cloudObject.setName(object);
                assertObjectExists("No file for downloading: " + cloudObject, cloud, cloudObject.getDirectory(), cloudObject.getName(), false);
                task = cloud.getStorageServices().getBlobStoreSupport().download(cloudObject, targetFile);
            }
            else {
                assertObjectExists("No file for downloading: " + directory + "/" + object, cloud, directory, object, true);
                task = cloud.getStorageServices().getBlobStoreSupport().download(directory, object, targetFile, encryption);
            }
            while( !task.isComplete() ) {
                try { Thread.sleep(1000L); }
                catch( InterruptedException e ) { }
            }
            if( task.getTransferError() != null ) {
                throw new RuntimeException(task.getTransferError());
            }
            assertTrue("File was corrupted", checkTestFile(targetFile));
        }
        finally {
            if( targetFile.exists() ) {
                targetFile.delete();
            }
        }
    }
    
    private void list(String directory, String ... expected) throws CloudException, InternalException {
        int expectedFound = 0;
        
        if( expected == null ) {
            expected = new String[0];
        }
        long timeout = System.currentTimeMillis() + (CalendarWrapper.MINUTE * 2L);
        
        while( System.currentTimeMillis() < timeout ) {
            for( CloudStoreObject item : cloud.getStorageServices().getBlobStoreSupport().listFiles(directory) ) {
                if( item.getDirectory() == null ) {
                    out("  " + item.getName());
                }
                else {
                    out("  /" + item.getDirectory() + "/" + item.getName());
                }
                for( String name : expected ) {
                    if( name.equals(item.getName()) ) {
                        expectedFound++;
                        break;
                    }
                }
            }
            if( expectedFound == expected.length ) {
                return;
            }
        }
        assertTrue("Expected files were not found (" + expected.length + " vs " + expectedFound + ")", expectedFound == expected.length);
    }
    
    @Before
    @Override
    public void setUp() throws InstantiationException, IllegalAccessException, CloudException, InternalException {
        String name = getName();
        
        cloud = getProvider();
        cloud.connect(getTestContext());
        if( name.equals("testCreateChildDirectory") || name.equals("testUploadFileToRoot") || name.equals("testUploadFileToChild") || name.equals("testUploadEncryptedFile") || name.equals("testUploadMultipartFile") || name.equals("testDownloadRootFile") || name.equals("testDownloadChildFile") || name.equals("testDownloadEncryptedFile") || name.equals("testDownloadMultipartFile") || name.equals("testListRoot") || name.equals("testListChild") || name.equals("testListThirdGen") || name.equals("testRemoveDirectory") || name.equals("testRemoveFile") || name.equals("testRemoveMultipart") || name.equals("testClearChild") || name.equals("testClearRoot") ) {
            directoryToRemove = cloud.getStorageServices().getBlobStoreSupport().createDirectory("dsnccd" + System.currentTimeMillis(), true);
            testDirectory = directoryToRemove;
        }
        if( name.equals("testUploadFileToRoot") || name.equals("testUploadFileToChild") || name.equals("testUploadEncryptedFile") || name.equals("testUploadMultipartFile") ) {
            testLocalFile = createTestFile();
        }
        if( name.equals("testDownloadRootFile") || name.equals("testDownloadChildFile") || name.equals("testDownloadEncryptedFile") || name.equals("testDownloadMultipartFile") || name.equals("testListChild") || name.equals("testListThirdGen") || name.equals("testRemoveFile") || name.equals("testRemoveMultipart") ) {
            testFileName = "dsnroot" + System.currentTimeMillis() + ".txt";
        }
        if( name.equals("testUploadFileToChild") || name.equals("testUploadEncryptedFile") || name.equals("testUploadMultipartFile") || name.equals("testDownloadChildFile") || name.equals("testDownloadEncryptedFile") || name.equals("testDownloadMultipartFile") || name.equals("testListThirdGen") ) {
            testDirectory = cloud.getStorageServices().getBlobStoreSupport().createDirectory(testDirectory + ".dsnufc" + System.currentTimeMillis(), true);
        }
        if( name.equals("testDownloadRootFile") || name.equals("testDownloadChildFile") || name.equals("testListChild") || name.equals("testListThirdGen") || name.equals("testRemoveFile") ) {
            testLocalFile = createTestFile();
            cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, testFileName, false, null);
        }
        if( name.equals("testClearChild") ) {
            testFileName = cloud.getStorageServices().getBlobStoreSupport().createDirectory(testDirectory + ".childdir", true);
            testFileName = testFileName.substring(testFileName.lastIndexOf('.') + 1);
            testLocalFile = createTestFile();
            cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory + "." + testFileName, "testfile.txt", false, null);
        }
        if( name.equals("testClearRoot") ) {
            testLocalFile = createTestFile();
            cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, "testfile.txt", false, null);            
        }
        if( name.equals("testDownloadEncryptedFile") ) {
            testLocalFile = createTestFile();
            cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, testFileName, true, new StorageEncryption());
        }
        if( name.equals("testDownloadMultipartFile") || name.equals("testRemoveMultipart") ) {
            testLocalFile = createTestFile();
            cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, testFileName, true, null);
        }
    }
    
    @After
    @Override
    public void tearDown() {
        try {
            if( directoryToRemove != null ) {
                cloud.getStorageServices().getBlobStoreSupport().clear(directoryToRemove);
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( cloud != null ) {
                cloud.close();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
        try {
            if( testLocalFile != null ) {
                testLocalFile.delete();
            }
        }
        catch( Throwable ignore ) {
            // ignore
        }
    }
    
    @Test
    public void testClearChild() throws CloudException, InternalException {
        cloud.getStorageServices().getBlobStoreSupport().clear(testDirectory + "." + testFileName);
        assertTrue("Did not clear directory", !cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory + "." + testFileName));
        assertTrue("Parent directort is not there!", cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory));
    }
    
    @Test
    public void testClearRoot() throws CloudException, InternalException {
        cloud.getStorageServices().getBlobStoreSupport().clear(testDirectory);
        assertTrue("Did not clear directory", !cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory));
        directoryToRemove = null;
        testDirectory = null;
    }
    
    @Test
    public void testCreateChildDirectory() throws InternalException, CloudException {
        begin();
        String dir = "dsncrd" + System.currentTimeMillis();
        
        list(testDirectory);
        dir = cloud.getStorageServices().getBlobStoreSupport().createDirectory(testDirectory + "." + dir, true);
        list(testDirectory);
        assertNotNull("No directory was created", dir);
        assertDirectoryExists("Directory " + dir + " was not created in " + testDirectory + ".", cloud, dir);
        end();
    }
 
    @Test
    public void testCreateRootDirectory() throws InternalException, CloudException {
        begin();
        String dir = "dsncrd" + System.currentTimeMillis();
        
        directoryToRemove = cloud.getStorageServices().getBlobStoreSupport().createDirectory(dir, true);
        assertNotNull("No directory was created", directoryToRemove);
        assertDirectoryExists("Directory " + directoryToRemove + " does not exist", cloud, directoryToRemove);
        end();
    }
    
    @Test
    public void testDownloadChildFile() throws Exception {
        begin();
        download(testDirectory, testFileName, false, null);
        end();
    }
    
    @Test
    public void testDownloadEncryptedFile() throws Exception {
        begin();
        download(testDirectory, testFileName, true, new StorageEncryption());
        end();
    }
    
    @Test
    public void testDownloadMultipartFile() throws Exception {
        begin();
        download(testDirectory, testFileName, true, null);
        end();
    }
    
    @Test
    public void testDownloadRootFile() throws Exception {
        begin();
        download(testDirectory, testFileName, false, null);
        end();
    }
    
    @Test
    public void testListChild() throws CloudException, InternalException {
        begin();
        list(testDirectory, testFileName);
        end();
    }

    @Test
    public void testListRoot() throws CloudException, InternalException {
        begin();
        list(null, testDirectory);
        end();
    }
    
    @Test
    public void testListThirdGen() throws CloudException, InternalException {
        begin();
        list(testDirectory, testFileName);
        end();
    }
    
    @Test
    public void testRemoveDirectory() throws CloudException, InternalException {
        begin();
        cloud.getStorageServices().getBlobStoreSupport().removeDirectory(testDirectory);
        assertTrue("Directory " + testDirectory + " still exists", !cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory));
        directoryToRemove = null;
        testDirectory = null;
        end();
    }
    
    @Test
    public void testRemoveFile() throws CloudException, InternalException {
        begin();
        cloud.getStorageServices().getBlobStoreSupport().removeFile(testDirectory, testFileName, false);
        assertTrue("File " + testDirectory + "." + testFileName + " still exists", cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory, testFileName, false) < 0);
        testFileName = null;
        end();
    }
    
    @Test
    public void testRemoveMultipart() throws CloudException, InternalException {
        begin();
        cloud.getStorageServices().getBlobStoreSupport().removeFile(testDirectory, testFileName, true);
        assertTrue("File " + testDirectory + "." + testFileName + " still exists", cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory, testFileName, true) < 0);
        testFileName = null;
        end();
    }
    
    @Test
    public void testSubscription() throws CloudException, InternalException {
        begin();
        assertTrue("Account is not subscribed, tests will be invalid", cloud.getStorageServices().getBlobStoreSupport().isSubscribed());
        end();
    }
    
    @Test
    public void testUploadEncryptedFile() throws InternalException, CloudException {
        begin();
        Encryption encryption = new StorageEncryption();
        String fileName = "encrypted-test.txt";

        cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, fileName, true, encryption);
        assertObjectExists("File does not exist in cloud", cloud, testDirectory, fileName, true);
        end();
    }
    
    @Test
    public void testUploadFileToChild() throws InternalException, CloudException {
        begin();
        String fileName = "child-test.txt";
        
        cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, fileName, false, null);
        assertTrue("File does not exist in cloud", cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory, fileName, false) > 0);
        end();
    }
    
    @Test
    public void testUploadFileToRoot() throws InternalException, CloudException {
        begin();
        String fileName = "root-test.txt";
        
        cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, fileName, false, null);
        assertTrue("File does not exist in cloud", cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory, fileName, false) > 0);
        end();
    }
    
    @Test
    public void testUploadMultipartFile() throws InternalException, CloudException {
        begin();
        String fileName = "multi-test.txt";
        
        cloud.getStorageServices().getBlobStoreSupport().upload(testLocalFile, testDirectory, fileName, true, null);
        assertTrue("File does not exist in cloud", cloud.getStorageServices().getBlobStoreSupport().exists(testDirectory, fileName + ".properties", false) > 0);
        end();
    }
}
