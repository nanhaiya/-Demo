package com.iflytek.voicecloud.lfasr.demo;

import java.io.*;
import java.util.HashMap;

import com.alibaba.fastjson.JSON;
import com.iflytek.msp.cpdb.lfasr.client.LfasrClientImp;
import com.iflytek.msp.cpdb.lfasr.exception.LfasrException;
import com.iflytek.msp.cpdb.lfasr.model.LfasrType;
import com.iflytek.msp.cpdb.lfasr.model.Message;
import com.iflytek.msp.cpdb.lfasr.model.ProgressStatus;

/**
 * 非实时转写SDK调用demo
 * 此demo只是一个简单的调用示例, 不适合用到实际生产环境中
 *
 * @author white
 *
 */
public class LfasrSDKDemo {


    // 原始音频存放地址，文本路径为原始路径，格式txt
    private static final String local_file = "F:/laen/lean/read/text2.mp3";

    /*
     * 转写类型选择：标准版和电话版(旧版本, 不建议使用)分别为：
     * LfasrType.LFASR_STANDARD_RECORDED_AUDIO 和 LfasrType.LFASR_TELEPHONY_RECORDED_AUDIO
     * */
    private static final LfasrType type = LfasrType.LFASR_STANDARD_RECORDED_AUDIO;

    // 等待时长（秒）
    private static int sleepSecond = 1;

    public static void main(String[] args) {
        // 初始化LFASRClient实例
        LfasrClientImp lc = null;
        try {
            lc = LfasrClientImp.initLfasrClient();
        } catch (LfasrException e) {
            // 初始化异常，解析异常描述信息
            Message initMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + initMsg.getErr_no());
            System.out.println("failed=" + initMsg.getFailed());
        }

        // 获取上传任务ID
        String task_id = "";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("has_participle", "true");
        //合并后标准版开启电话版功能
        //params.put("has_seperate", "true");
        try {
            // 上传音频文件
            Message uploadMsg = lc.lfasrUpload(local_file, type, params);

            // 判断返回值
            int ok = uploadMsg.getOk();
            if (ok == 0) {
                // 创建任务成功
                task_id = uploadMsg.getData();
                System.out.println("task_id=" + task_id);
            } else {
                // 创建任务失败-服务端异常
                System.out.println("ecode=" + uploadMsg.getErr_no());
                System.out.println("failed=" + uploadMsg.getFailed());
            }
        } catch (LfasrException e) {
            // 上传异常，解析异常描述信息
            Message uploadMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + uploadMsg.getErr_no());
            System.out.println("failed=" + uploadMsg.getFailed());
        }

        // 循环等待音频处理结果
        while (true) {
            try {
                // 等待20s在获取任务进度
                Thread.sleep(sleepSecond * 1000);
                System.out.println("waiting ...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                // 获取处理进度
                Message progressMsg = lc.lfasrGetProgress(task_id);

                // 如果返回状态不等于0，则任务失败
                if (progressMsg.getOk() != 0) {
                    System.out.println("task was fail. task_id:" + task_id);
                    System.out.println("ecode=" + progressMsg.getErr_no());
                    System.out.println("failed=" + progressMsg.getFailed());

                    return;
                } else {
                    ProgressStatus progressStatus = JSON.parseObject(progressMsg.getData(), ProgressStatus.class);
                    if (progressStatus.getStatus() == 9) {
                        // 处理完成
                        System.out.println("task was completed. task_id:" + task_id);
                        break;
                    } else {
                        // 未处理完成
                        System.out.println("task is incomplete. task_id:" + task_id + ", status:" + progressStatus.getDesc());
                        continue;
                    }
                }
            } catch (LfasrException e) {
                // 获取进度异常处理，根据返回信息排查问题后，再次进行获取
                Message progressMsg = JSON.parseObject(e.getMessage(), Message.class);
                System.out.println("ecode=" + progressMsg.getErr_no());
                System.out.println("failed=" + progressMsg.getFailed());
            }
        }

        String saveFie=(local_file.substring(0,local_file.lastIndexOf("/")+1))+local_file.substring(local_file.lastIndexOf("/")+1,local_file.lastIndexOf("."))+"-语音转文本.txt";

        // 获取任务结果
        try {
            Message resultMsg = lc.lfasrGetResult(task_id);
            // 如果返回状态等于0，则获取任务结果成功
            if (resultMsg.getOk() == 0) {
//                resultMsg.toString()
                System.out.println(resultMsg.toString());
                //处理结果文本，用end字符串储存
                String sucess=resultMsg.getData();
                int onebest = 1;
                int si=1;
                String end="";
                do {
                    onebest = sucess.indexOf("onebest",onebest) + 10;
                    si = sucess.indexOf("si",si) - 3;
                    end += sucess.substring(onebest, si);
                    si+=6;
                }while(sucess.indexOf("onebest",onebest)>0);

                //处理储存文本路径
                File file;
                int num=1;
                do{
                    file=new File(saveFie);
                    if(file.exists()){
                        saveFie=(local_file.substring(0,local_file.lastIndexOf("/")+1))+local_file.substring(local_file.lastIndexOf("/")+1,local_file.lastIndexOf("."))+"-语音转文本（"+num+"）.txt";
                    }
                    num++;
                }while(file.exists());
                 //写入文件
                PrintStream ps = new PrintStream(new FileOutputStream(file));
                ps.append(end);
//
//                // 打印转写结果
//                System.out.println(resultMsg.getData());
            } else {
                // 获取任务结果失败
                System.out.println("ecode=" + resultMsg.getErr_no());
                System.out.println("failed=" + resultMsg.getFailed());
            }
        } catch (LfasrException | IOException e) {
            // 获取结果异常处理，解析异常描述信息
            Message resultMsg = JSON.parseObject(e.getMessage(), Message.class);
            System.out.println("ecode=" + resultMsg.getErr_no());
            System.out.println("failed=" + resultMsg.getFailed());
        }
    }
}
