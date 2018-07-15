package com.dewarim.cinnamon.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ConcurrentHashSet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockService {

    private static final Logger log = LogManager.getLogger(LockService.class);

    private final Map<Long, List<ContentLock>> locks = new ConcurrentHashMap<>();

    private final Object LOCK           = new Object();
    private       long   WAITING_PERIOD = 300_000L; // default: 5 min
    private       long   CHECK_PERIOD   = 1000L;


    private boolean gotVerifyLock(long id) {
        synchronized (LOCK) {
            List<ContentLock> contentLocks = locks.get(id);

            /*
             * Look if other threads have a verify or write lock.
             * This would mean another thread is currently verifying or
             * updating the meta and / or content data.
             */
            if (contentLocks.stream().anyMatch(lock -> lock.verifyLocked || lock.writeLocked)) {
                return false;
            }

            // no rivaling threads here - good to go:
            ContentLock lock = new ContentLock(id);
            lock.setVerifyLocked(true);
            contentLocks.add(lock);
            locks.put(id, contentLocks);
            return true;
        }
    }

    private boolean gotWriteLock(long id) {
        synchronized (LOCK) {
            List<ContentLock> contentLocks = locks.get(id);

            /*
             * Current state: we got a verify lock and want to switch to write lock.
             * We must wait until all other threads are no longer reading from this object
             * before we overwrite the file.
             */
            if (contentLocks.stream().anyMatch(lock -> lock.readLocked)) {
                return false;
            }


            long threadId = Thread.currentThread().getId();
            contentLocks.stream()
                    .filter(lock -> lock.threadId == threadId)
                    .findFirst()
                    .ifPresent(lock -> {
                                lock.setVerifyLocked(false);
                                lock.setWriteLocked(true);
                                lock.setReadLocked(false);
                            }
                    );
            return true;
        }
    }

    /**
     * After verifying that an object's local content is current with the master server's version,
     * downgrade the thread's lock to a read-lock (from either write or verifying lock).
     *
     * @param id the Cinnamon object's id
     */
    public void switchToReadLock(long id) {
        /*
         * Current state: this thread has either verify or write lock.
         * Downgrading the lock should always be possible.
         */
        synchronized (LOCK) {
            long              threadId     = Thread.currentThread().getId();
            List<ContentLock> contentLocks = locks.get(id);
            contentLocks.stream()
                    .filter(lock -> lock.threadId == threadId)
                    .findFirst()
                    .ifPresent(lock -> {
                                lock.setVerifyLocked(false);
                                lock.setWriteLocked(false);
                                lock.setReadLocked(true);
                            }
                    );
        }
    }

    /**
     * Upgrade this thread's verify lock to a write lock and store the newer version of
     * the object's content.
     *
     * @param id the Cinnamon object's id
     */
    public void switchToWriteLock(long id) throws IOException {

        long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < WAITING_PERIOD) {
                log.debug("sleep for {}", CHECK_PERIOD);
                Thread.sleep(CHECK_PERIOD);

                if (gotWriteLock(id)) {
                    log.debug("waiting is over, got write lock");
                    return;
                }
            }
            log.warn("Request timed out for object #{}", id);
            throw new IOException("Request timed out.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }


    }

    /**
     * Try to get a verify lock so this thread can check if the local cache content is current.
     * If another thread is already verifying the object, wait until we get a read lock.
     *
     * @param id the Cinnamon object's id
     */
    public void switchToVerifyLock(long id) throws IOException {
        synchronized (LOCK) {
            List<ContentLock> contentLocks = locks.getOrDefault(id, new ArrayList<>());
            if (contentLocks.isEmpty()) {
                ContentLock lock = new ContentLock(id);
                lock.setVerifyLocked(true);
                contentLocks.add(lock);
                locks.put(id, contentLocks);
                return;
            }
        }

        long startTime = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - startTime < WAITING_PERIOD) {
                log.debug("sleep for {}", CHECK_PERIOD);
                Thread.sleep(CHECK_PERIOD);

                if (gotVerifyLock(id)) {
                    log.debug("waiting is over, got verify lock");
                    return;
                }
            }
            log.warn("Request timed out for object #{}", id);
            throw new IOException("Request timed out.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }


    /**
     * Remove all locks held by this thread for the given object id.
     *
     * @param id the Cinnamon object's id
     */
    public void removeLocks(long id) {
        synchronized (LOCK) {
            ContentLock key = new ContentLock(id);
            locks.getOrDefault(id, Collections.emptyList()).remove(key);
        }
    }

    private static class ContentLock {
        private long    threadId;
        private long    id;
        private boolean readLocked;
        private boolean writeLocked;
        private boolean verifyLocked;


        ContentLock(long id) {
            threadId = Thread.currentThread().getId();
            this.id = id;
        }

        public long getThreadId() {
            return threadId;
        }

        public long getId() {
            return id;
        }

        public boolean isReadLocked() {
            return readLocked;
        }

        public void setReadLocked(boolean readLocked) {
            this.readLocked = readLocked;
        }

        public boolean isWriteLocked() {
            return writeLocked;
        }

        public void setWriteLocked(boolean writeLocked) {
            this.writeLocked = writeLocked;
        }

        public boolean isVerifyLocked() {
            return verifyLocked;
        }

        public void setVerifyLocked(boolean verifyLocked) {
            this.verifyLocked = verifyLocked;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ContentLock that = (ContentLock) o;
            return threadId == that.threadId &&
                    id == that.id;
        }

        @Override
        public int hashCode() {

            return Objects.hash(threadId, id);
        }
    }


}
