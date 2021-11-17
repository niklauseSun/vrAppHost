package com.quick.jsbridge.takeToSee;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.collection.ArraySet;

import com.quick.jsbridge.view.IQuickFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class FileSplit implements IRtcImpl {

    private static ArrayList<File> fileList;
    private static ArrayList<String> md5List;

    public static HashMap calculateFile(String filePath, Context context) {
        File targetFile = new File(filePath);
        int count = getSplitFile(targetFile, 1* 1024 * 1024);

        if (!fileList.isEmpty()) {
            String totalMd = file2Md5(targetFile);
            String fistMd = file2Md5(fileList.get(0));
            String lastMd = file2Md5(fileList.get(count - 1));

            HashMap map = new HashMap();
            map.put("type", "success");
            map.put("totalMd5", totalMd);
            map.put("firstMd5", fistMd);
            map.put("lastMd5", lastMd);
            map.put("fileList", fileList);
            return map;
        }

       return new HashMap();
    }

    public static void saveFileList(Context context) {
        SharedPreferences pr = context.getSharedPreferences("splitFileList", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = pr.edit();

        ed.putStringSet("fileSplitList", new HashSet(Arrays.asList(fileList)));
    }

    public static void uploadMd5(String reqUrl, String type, int index, IQuickFragment webLoader) {
        String md5String = md5List.get(index);
        UploadInstance.uploadMd5String(reqUrl, type, md5String, webLoader);
    }

    public static int getSplitFile(File targetFile, long cutSize) {
        // 计算切割文件大小
        int count = targetFile.length() % cutSize == 0 ?(int) (targetFile.length()/ cutSize): (int) (targetFile.length() / cutSize + 1);

        RandomAccessFile raf = null;
        try {
            //获取目标文件 预分配文件所占的空间 在磁盘中创建一个指定大小的文件  r 是只读
            raf = new RandomAccessFile(targetFile, "r");
            long length = raf.length(); // 文件总长度
            long maxSize = length / count;
            long offSet = 0L;
            for (int i = 0;i < count - 1; i++) { // 最后一片单独处理
                long begin = offSet;
                long end = (i + 1) * maxSize;
                offSet = getWrite(targetFile.getAbsolutePath(), i, begin, end);
            }

            if ((length - offSet) > 0) {
                getWrite(targetFile.getAbsolutePath(), count-1, offSet, length);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }

    public static long getWrite(String file,int index, long begin, long end) {
        long endPointer = 0L;
        String a = file.split(suffixName(new File(file)))[0];
        try {
            // 申明文件切割后的文件磁盘
            RandomAccessFile in = new RandomAccessFile(new File(file), "r");
            // 定义一个可读，可写的文件后缀名为.tmp的二进制文件
            // 读取切片文件
            File mFile = new File(a + "_" + index + ".tmp");

//            String md5String = file2Md5(mFile);

            fileList.add(mFile);
//            md5List.add(md5String);

            // 如果存在
            if (!fileIsExists(file)) {
                RandomAccessFile out = new RandomAccessFile(mFile, "w");
                // 申明具体每一文件的字节数组
                byte[] bytes = new byte[1024];
                int n = 0;

                // 从指定位置读取文件字节流
                in.seek(begin);
                // 判断文件流读取的边界
                while ((n = in.read(bytes)) != -1 && in.getFilePointer() <= end) {
                    // 从指定每一份文件的范围，写入不同的文件
                    out.write(bytes, 0, n);
                }
                //定义当前读取文件的指针
                endPointer = in.getFilePointer();
                // 关闭输入流
                in.close();
                // 关闭输出流
                out.close();
            } else {
                //不存在
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return endPointer - 1024;
    }

    public static String suffixName(File file) {
        String fileName = file.getName();
        String fileType = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        return fileType;
    }

    private static boolean fileIsExists(String filePath) {
        try {
            File f = new File(filePath);
            if(!f.exists())
            {
                return false;
            }
        } catch (Exception e)
        {
            return false;
        }
        return true;
    }

    private static String file2Md5(File file) {
        int bufferSize = 1024;
        FileInputStream fis = null;
        DigestInputStream dis = null;

        try {
            // 创建MD5转换器和文件流
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            dis = new DigestInputStream(fis, messageDigest);

            byte[] buffer = new byte[bufferSize];
            // DigestInputStream实际上在流处理文件时就在内部进行了一定的处理
            while (dis.read(buffer) > 0);

            // 通过DigestInputStream拿到结果，也是字节数组，包含16个元素
            byte[] array = messageDigest.digest();
            // 同样,把字节数组转换成字符串
            StringBuffer hex = new StringBuffer(array.length * 2);
            for (byte b : array) {
                if ((b & 0xFF) < 0x10) {
                    hex.append("0");
                }
                hex.append(Integer.toHexString(b & 0xFF));
            }

            return hex.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
