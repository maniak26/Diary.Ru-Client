package adonai.diary_browser;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class CacheManager 
{
	private static CacheManager mInstance;

    public static long MAX_SIZE = 5 * 1048576L; // 5MB
    // загруженные странички
    private Map<String, Object> browseCache = new HashMap<>();

    public Object loadPageFromCache(String URL)
    {
        return browseCache.get(URL);
    }

    public boolean hasPage(String URL)
    {
    	return browseCache.containsKey(URL);
    }

    public void putPageToCache(String URL, Object page)
    {
    	browseCache.put(URL, page);
    }

    public void clear()
    {
        browseCache.clear();
    }
    
    public static CacheManager getInstance()
    {
    	if(mInstance == null)
    		mInstance = new CacheManager();
    	return mInstance;
    }

    public void cacheData(Context context, byte[] data, String name) throws IOException 
    {

        File cacheDir = context.getCacheDir();
        long size = getDirSize(cacheDir);
        long newSize = data.length + size;

        if (newSize > MAX_SIZE)
            cleanDir(cacheDir, newSize - MAX_SIZE);

        File file = new File(cacheDir, name);
        FileOutputStream os = new FileOutputStream(file);
        try 
        {
            os.write(data);
        }
        finally 
        {
            os.flush();
            os.close();
        }
    }

    public byte[] retrieveData(Context context, String name) throws IOException 
    {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        if (!file.exists()) 
            return null;

        byte[] data = new byte[(int) file.length()];
        FileInputStream is = new FileInputStream(file);
        try 
        {
            is.read(data);
        }
        finally 
        {
            is.close();
        }

        return data;
    }
    
    public boolean hasData(Context context, String name) 
    {

        File cacheDir = context.getCacheDir();
        File file = new File(cacheDir, name);

        // Data doesn't exist
        return file.exists();

    }
    
    public static File saveDataToSD(Context context, String name, InputStream inStream)
    {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;
        
        File SD = Environment.getExternalStorageDirectory();
        File externalDir = new File(SD, "Diary.Ru"); 
        if(!externalDir.exists())
            externalDir.mkdir();
        
        File toFile = new File(externalDir, name);
        
        try 
        {
            final OutputStream out = new FileOutputStream(toFile);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = inStream.read(buf)) > 0)
                out.write(buf, 0, len);
            inStream.close();
            out.close();
            Toast.makeText(context, context.getResources().getString(R.string.saved_to) + toFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return toFile;
        }  
        catch (IOException e) 
        {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public static File saveDataToSD(Context context, String name, byte[] bytes)
    {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        File SD = Environment.getExternalStorageDirectory();
        File externalDir = new File(SD, "Diary.Ru");
        if(!externalDir.exists())
            externalDir.mkdir();

        File toFile = new File(externalDir, name);

        try
        {
            final OutputStream out = new FileOutputStream(toFile);
            final InputStream inStream = new ByteArrayInputStream(bytes);

            final byte[] buf = new byte[1024];
            int len;
            while ((len = inStream.read(buf)) > 0)
                out.write(buf, 0, len);
            inStream.close();
            out.close();
            Toast.makeText(context, context.getResources().getString(R.string.saved_to) + toFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return toFile;
        }
        catch (IOException e)
        {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void cleanDir(File dir, long bytes)
    {

        long bytesDeleted = 0;
        File[] files = dir.listFiles();

        for (File file : files) 
        {
            bytesDeleted += file.length();
            file.delete();

            if (bytesDeleted >= bytes) 
                break;
        }
    }

    private long getDirSize(File dir) 
    {

        long size = 0;
        File[] files = dir.listFiles();

        for (File file : files) 
            if (file.isFile())
                size += file.length();

        return size;
    }
}