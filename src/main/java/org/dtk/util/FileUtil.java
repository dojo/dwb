package org.dtk.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

public class FileUtil {

	public static void writeToZipFile(String path, Map<String, byte[]> files) throws IOException {
		File file = new File(path);

		//Make sure destination dir exists.
		File parentDir = file.getParentFile();
		if(!parentDir.exists()){
			if(!parentDir.mkdirs()){
				throw new IOException("Could not create directory: " + parentDir.getAbsolutePath());
			}
		}

		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));

		Iterator<String> keys = files.keySet().iterator();

		while (keys.hasNext()) {
			String filename = keys.next();
			byte[] contents = files.get(filename);        	
			out.putNextEntry(new ZipEntry(filename));
			out.write(contents);
			out.closeEntry();
		}

		// Complete the ZIP file
		out.close();
	}

	public static String createTemporaryPackage(Map<String, String> packageModules) {
		String temporaryPackageId = null;
		Set<String> modulePaths = packageModules.keySet();

		try {
			File temporaryPackageLocation = createTempDirectory();

			for(String modulePath : modulePaths) {
				File moduleLocation = new File(temporaryPackageLocation, modulePath);
				writeToFile(moduleLocation.getAbsolutePath(), packageModules.get(modulePath), null, false);				
			}

			temporaryPackageId = temporaryPackageLocation.getName();
		} catch (IOException e) {
		}

		return temporaryPackageId;
	}

	public static void writeToFile(String path, String contents, String encoding, boolean useGzip) throws IOException {
		// summary: writes a file
		if (encoding == null) {
			encoding = "utf-8";
		}
		File file = new File(path);

		//Make sure destination dir exists.
		File parentDir = file.getParentFile();
		if(!parentDir.exists()){
			if(!parentDir.mkdirs()){
				throw new IOException("Could not create directory: " + parentDir.getAbsolutePath());
			}
		}

		OutputStream outStream = new FileOutputStream(file);
		if (useGzip) {
			outStream = new GZIPOutputStream(outStream);
		} else {

		}

		BufferedWriter output = new java.io.BufferedWriter(new OutputStreamWriter(outStream, encoding));
		try {
			output.append(contents);
		} finally {
			output.close();
		}            

	}

	/** 
	 * Generate a temporary file path by creating a new temporary file and removing. 
	 * 
	 * @param tempBuildPrefix - Temporary file prefix
	 * @param tempBuildSuffix - Temporary file suffix
	 * @return Temporary file path 
	 * @throws IOException - Couldn't create or remove temporary file
	 */
	public static String generateTemporaryFilePath(String tempPrefix, String tempSuffix) throws IOException {
		// Create temporary file path to hold the build 
		File cacheFile = File.createTempFile(tempPrefix, tempSuffix);
		String cacheFilePath = cacheFile.getAbsolutePath();
		// Remove temporary file until the build is completed. The existence of the file is 
		// used to tell the polling request when the build has completed.
		boolean confirmDelete = cacheFile.delete();        		
		if (!confirmDelete) {
			throw new IOException("Error removing temporary file, " + cacheFilePath);
		}

		return cacheFilePath;
	}

	public static File createTempDirectory() throws IOException {
		final File temp = File.createTempFile("dojo_web_builder", null);

		if(!(temp.delete())) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}

		if(!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}

		return (temp);
	}

	public static File[] findAllDirectories(File parentDirectory) {
		File[] childDirectories = null; 

		// Sanity check argument, mustn't be null and must be a directory
		// to contain child files.
		if (parentDirectory != null && parentDirectory.isDirectory()) {
			childDirectories = parentDirectory.listFiles(new FileFilter() {				
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory();
				}
			});			
		}

		return childDirectories;
	}

	public static File[] findAllPathSuffixMatches(File parentDirectory, final String pathSuffix) {
		File[] childDirectories = null; 

		// Sanity check argument, mustn't be null and must be a directory
		// to contain child files.
		if (parentDirectory != null && parentDirectory.isDirectory()) {
			childDirectories = parentDirectory.listFiles(new FileFilter() {				
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(pathSuffix);
				}
			});			
		}

		return childDirectories;
	}

	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++) {
				if(files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return( path.delete() );
	}	


	public static String inflateTemporaryZipFile(InputStream is) throws IOException {
		File temporaryDir = FileUtil.createTempDirectory();
		String temporaryUserAppPath = temporaryDir.getAbsolutePath();

		boolean success = FileUtil.inflateZipFile(temporaryUserAppPath, is);

		if (success) {
			return temporaryUserAppPath;
		} else {
			// Clean up temporary directory.
			FileUtil.deleteDirectory(temporaryDir);
			return null;
		}		
	}


	// TODO: Must handle errors properly at this stage. 
	public static boolean inflateZipFile(String baseDirectory, InputStream is) {
		boolean success = false;

		ZipInputStream zis = new ZipInputStream(is);

		ZipEntry zipEntry;
		try {
			zipEntry = zis.getNextEntry();

			while (zipEntry != null) {						        
				if (zipEntry.isDirectory()) {
					File dirFile = new File(baseDirectory, zipEntry.getName());	
					dirFile.mkdir();
				} else {
					FileOutputStream fout = new FileOutputStream(new File(baseDirectory, zipEntry.getName()));		        	
					for (int c = zis.read(); c != -1; c = zis.read()) {
						fout.write(c);
					}
					fout.close();
				}
				zis.closeEntry();		        
				zipEntry = zis.getNextEntry();
				// Ensure we have successfully inflated at least one entry, removes.
				success = true;
			}
			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
		} 		

		return success;
	}

	/**
	 * Return streaming output instance for a given file path.
	 * 
	 * @param filename - File to stream
	 * @param removeOnFinish - Delete file after streaming? 
	 * @return File streaming output.
	 */
	public static StreamingOutput streamingFileOutput(final String filename, final boolean removeOnFinish) {
		return new StreamingOutput() {			
			public void write(OutputStream output) throws IOException, WebApplicationException {
				// Read file, write to output buffer and close stream...
				File file = new File(filename);

				BufferedInputStream in = new java.io.BufferedInputStream(
						new DataInputStream(new FileInputStream(file)));                
				byte[] bytes = new byte[64000];
				int bytesRead = in.read(bytes);

				while (bytesRead != -1) {
					output.write(bytes, 0, bytesRead);
					bytesRead = in.read(bytes);
				}        

				in.close();

				// Remove file after streaming completes?
				if (removeOnFinish) {
					file.delete();
				}
			}
		};
	}
	
	/**
	 * Substitute any environment variables in the file path
	 * for actual values. 
	 * 
	 * @param filePath - String that may contain environment variables
	 * @return String Resolved file path
	 */
	public static String resolveEnvironmentVariables(String filePath) {		
		String resolvedPath = null;
		
		// Can't resolve empty values!
		if (filePath != null) {
			// Position index to allow copying of non-matching path sections
			int lastMatch = 0;
			StringBuilder pathBuilder = new StringBuilder();
			
			Pattern pattern = Pattern.compile("\\%(.+)\\%");
			Matcher matcher = pattern.matcher(filePath);
			
			while(matcher.find()) {				
				// Add all file path sections after last match but before current match.
				pathBuilder.append(filePath.substring(lastMatch, matcher.start()));
				
				// Add resolved environment variable path
				String envVarName = filePath.substring(matcher.start(1), matcher.end(1));
				pathBuilder.append(System.getenv(envVarName));
				
				// Advanced index past the end of environment variable.
				lastMatch = matcher.end();
			}
			
			// Finally, all all remaining path sections & create full resolved path
			pathBuilder.append(filePath.substring(lastMatch, filePath.length()));
			resolvedPath = pathBuilder.toString();
		}
		
		return resolvedPath;
	}
}
