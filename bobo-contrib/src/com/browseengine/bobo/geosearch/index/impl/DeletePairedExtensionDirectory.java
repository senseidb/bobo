package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

/**
 * A directory implementation that wraps another directory
 * and pairs two extensions together on delete.  
 * For each paiered extension (A, B), anytime fileName.B is deleted, 
 * PairedDeleteDirectory will first delete fileName.A, if it exists 
 * 
 * @author Geoff Cooney
 *
 */
public class DeletePairedExtensionDirectory extends Directory {
    private final Directory actualDirectory;
    private final Map<String, String> pairedDeleteExtensions;
    
    public DeletePairedExtensionDirectory(Directory directory) {
        this.actualDirectory = directory;
        this.pairedDeleteExtensions =  new HashMap<String, String>();
    }

    public void addExtensionPairing(String extFrom, String extTo) {
        pairedDeleteExtensions.put(extFrom, extTo);
    }
    
    @Override
    public String[] listAll() throws IOException {
        return actualDirectory.listAll();
    }

    @Override
    public boolean fileExists(String name) throws IOException {
        return actualDirectory.fileExists(name);
    }

    @Override
    public long fileModified(String name) throws IOException {
        return actualDirectory.fileModified(name);
    }

    @Override
    public void touchFile(String name) throws IOException {
        actualDirectory.touchFile(name);
    }

    @Override
    public void deleteFile(String name) throws IOException {
        String ext = FilenameUtils.getExtension(name);
        
        String pairedExt = pairedDeleteExtensions.get(ext);
        if (StringUtils.isNotBlank(pairedExt)) {
            String pairedFileName = FilenameUtils.removeExtension(name) +
                FilenameUtils.EXTENSION_SEPARATOR_STR +
                pairedExt;
            
            if (fileExists(pairedFileName)) {
                deleteFile(pairedFileName);
            }
        }
        
        actualDirectory.deleteFile(name);
    }

    @Override
    public long fileLength(String name) throws IOException {
        return actualDirectory.fileLength(name);
    }

    @Override
    public IndexOutput createOutput(String name) throws IOException {
        return actualDirectory.createOutput(name);
    }

    @Override
    public void sync(String name) throws IOException {
        actualDirectory.sync(name);
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        actualDirectory.sync(names);
    }

    @Override
    public IndexInput openInput(String name) throws IOException {
        return actualDirectory.openInput(name);
    }

    @Override
    public IndexInput openInput(String name, int bufferSize) throws IOException {
        return actualDirectory.openInput(name, bufferSize);
    }

    @Override
    public Lock makeLock(String name) {
        return actualDirectory.makeLock(name);
    }

    @Override
    public void clearLock(String name) throws IOException {
        actualDirectory.clearLock(name);
    }

    @Override
    public void close() throws IOException {
        actualDirectory.close();
    }

    @Override
    public void setLockFactory(LockFactory lockFactory) throws IOException {
        actualDirectory.setLockFactory(lockFactory);
    }

    @Override
    public LockFactory getLockFactory() {
        return actualDirectory.getLockFactory();
    }

    @Override
    public String getLockID() {
        return actualDirectory.getLockID();
    }

    @Override
    public String toString() {
        return actualDirectory.toString();
    }

    @Override
    public void copy(Directory to, String src, String dest) throws IOException {
        actualDirectory.copy(to, src, dest);
    }
}
