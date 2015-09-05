package simpledb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {

	public ConcurrentHashMap<PageId, ArrayList<TransactionId>> sharedLocks;
	public ConcurrentHashMap<PageId, TransactionId> exclusiveLocks;

	public LockManager(){
		sharedLocks = new ConcurrentHashMap<PageId, ArrayList<TransactionId>>();
		exclusiveLocks = new ConcurrentHashMap<PageId, TransactionId>(); 
	}

	public synchronized boolean hasExclusiveLock(PageId pid, TransactionId tid){
		return exclusiveLocks.containsKey(pid) && exclusiveLocks.get(pid).equals(tid); 
	}

	public synchronized boolean hasSharedLock(PageId pid, TransactionId tid){
		return sharedLocks.containsKey(pid) &&  sharedLocks.get(pid).contains(tid);
	}

	//True when tid is the only one holding a shared lock on pid
	public synchronized boolean isUpgradable(PageId pid, TransactionId tid){
		return hasSharedLock(pid, tid) && (sharedLocks.get(pid).size() == 1);
	}

}