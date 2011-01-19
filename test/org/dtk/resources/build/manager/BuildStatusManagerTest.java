package org.dtk.resources.build.manager;

import java.util.Date;

import org.dtk.resources.build.BuildRequest;
import org.dtk.resources.exceptions.MissingResourceException;

public class BuildStatusManagerTest {
	
	final class RThread extends Thread {
		Integer index = -1;
		
		public RThread(Integer index) {
			this.index = index;
		}
		
		public void run() {
			System.out.println(index + " Reader starting...." + new Long(System.nanoTime()));
			BuildStatusManager bsm = BuildStatusManager.getInstance();	
			BuildState bs = BuildState.NOT_STARTED;
			
			try {
				bs = bsm.retrieveBuildState("A");
			} catch(MissingResourceException mre) {
				System.out.println(index + " First Not added!" + new Long(System.nanoTime()));
			}
			
			
			Integer counter = 0;
			while (bs != BuildState.BUILDING) {
				System.out.println(index + " S Reader found: " + bs.toString() + " #" + new Long(System.nanoTime()));
				
				try {
					bs = bsm.retrieveBuildState("A");
				} catch(MissingResourceException mre) {
					System.out.println(index + " Not added!");
				}
				counter++;
			}
			System.out.println(index + " F Reader found: " + bs.toString() + " #" + new Long(System.nanoTime()));
		}
	}
	

	final class WThread extends Thread {
		Integer index = -1;
		
		public WThread(Integer index) {
			this.index = index;
		}
		
		public void run() {
			BuildStatusManager bsm = BuildStatusManager.getInstance();

			System.out.println(index + " Writer starting...." + new Long(System.nanoTime()));
			// TODO: Need to use basic dojo config.
			//bsm.scheduleBuildRequest(new BuildRequest());
			System.out.println(index + " Writer finished...." + new Long(System.nanoTime()));
		}
	};
	
	public void test_ReadersFirst(int readers) {
		WThread writer = new WThread(0);
		for (int i = 0; i < readers; i++) {
			(new RThread(i)).start();	
		}
		writer.start();
	}
	
	public void test_WriterFirst(int readers) {
		WThread writer = new WThread(0);
		for (int i = 0; i < readers; i++) {
			(new RThread(i)).start();	
		}
		writer.start();
	}
	
	public void test_ManyWriters(int writers) {
		for (int i = 0; i < writers; i++) {
			(new WThread(i)).start();	
		}
	}
	
	public void test_ManyRWs_WriteFirst(int writers) {
		for (int i = 0; i < writers; i++) {
			(new WThread(i)).start();	
			(new RThread(i)).start();	
		}
	}
	
	public void test_ManyRWs_WriteLast(int writers) {
		for (int i = 0; i < writers; i++) {
			(new RThread(i)).start();	
			(new WThread(i)).start();	
		}
	}
	
	public void test_ManyRWs_WriteAfter(int writers) {
		for (int i = 0; i < writers; i++) {
			(new RThread(i)).start();	
		}
		for (int i = 0; i < writers; i++) {
			(new WThread(i)).start();	
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Starting....");

		BuildStatusManagerTest test = new BuildStatusManagerTest();

		test.test_ManyRWs_WriteAfter(100);
	}
}
