package cic.cs.unb.ca.ifm;

import cic.cs.unb.ca.flow.FlowMgr;
import cic.cs.unb.ca.jnetpcap.*;
import cic.cs.unb.ca.jnetpcap.worker.FlowGenListener;
import org.apache.commons.io.FilenameUtils;
import org.jnetpcap.PcapClosedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cic.cs.unb.ca.jnetpcap.worker.InsertCsvRow;
import swing.common.SwingUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static cic.cs.unb.ca.Sys.FILE_SEP;

public class Cmd {

    public static final Logger logger = LoggerFactory.getLogger(Cmd.class);
    private static final String DividingLine = "-------------------------------------------------------------------------------";
    private static String[] animationChars = new String[] { "|", "/", "-", "\\" };

    public static void main(String[] args) {

        long flowTimeout = 120000000L;
        long activityTimeout = 5000000L;
        String rootPath = System.getProperty("user.dir");
        String pcapPath;
        String outPath;

        /* Select path for reading all .pcap files */
        /*
         * if(args.length<1 || args[0]==null) {
         * pcapPath = rootPath+"/data/in/";
         * }else {
         * }
         */

        /* Select path for writing all .csv files */
        /*
         * if(args.length<2 || args[1]==null) {
         * outPath = rootPath+"/data/out/";
         * }else {
         * }
         */

        if (args.length < 1) {
            logger.info("Please select pcap!");
            return;
        }
        pcapPath = args[0];
        File in = new File(pcapPath);

        if (in == null || !in.exists()) {
            logger.info("The pcap file or folder does not exist! -> {}", pcapPath);
            return;
        }

        if (args.length < 2) {
            logger.info("Please select output folder!");
            return;
        }
        outPath = args[1];
        File out = new File(outPath);
        if (out == null || out.isFile()) {
            logger.info("The out folder does not exist! -> {}", outPath);
            return;
        }

        logger.info("You select: {}", pcapPath);
        logger.info("Out folder: {}", outPath);

        if (in.isDirectory()) {
            readPcapDir(in, outPath, flowTimeout, activityTimeout);
        } else {

            if (!SwingUtils.isPcapFile(in)) {
                logger.info("Please select pcap file!");
            } else {
                logger.info("CICFlowMeter received 1 pcap file");
                readPcapFile(in.getPath(), outPath, flowTimeout, activityTimeout);
            }
        }

    }

    private static void readPcapDir(File inputPath, String outPath, long flowTimeout, long activityTimeout) {
        if (inputPath == null || outPath == null) {
            return;
        }
        File[] pcapFiles = inputPath.listFiles(SwingUtils::isPcapFile);
        int file_cnt = pcapFiles.length;
        System.out.println(String.format("CICFlowMeter found :%d pcap files", file_cnt));
        for (int i = 0; i < file_cnt; i++) {
            File file = pcapFiles[i];
            if (file.isDirectory()) {
                continue;
            }
            int cur = i + 1;
            System.out.println(String.format("==> %d / %d", cur, file_cnt));
            readPcapFile(file.getPath(), outPath, flowTimeout, activityTimeout);

        }
        System.out.println("Completed!");
    }

    private static void readPcapFile(String inputFile, String outPath, long flowTimeout, long activityTimeout) {
        if (inputFile == null || outPath == null) {
            return;
        }
        String fileName = FilenameUtils.getName(inputFile);

        if (!outPath.endsWith(FILE_SEP)) {
            outPath += FILE_SEP;
        }

        File saveFileFullPath = new File(outPath + fileName + FlowMgr.FLOW_SUFFIX);

        if (saveFileFullPath.exists()) {
            if (!saveFileFullPath.delete()) {
                System.out.println("Save file can not be deleted");
            }
        }

        FlowGenerator flowGen = new FlowGenerator(true, flowTimeout, activityTimeout);
        flowGen.addFlowListener(new FlowListener(fileName, outPath));
        boolean readIP6 = false;
        boolean readIP4 = true;
        PacketReader packetReader = new PacketReader(inputFile, readIP4, readIP6);

        System.out.println(String.format("Working on... %s", fileName));

        int nValid = 0;
        int nTotal = 0;
        int nDiscarded = 0;
        long previousTimestamp = 0L;
        long currentTimestamp = 0L;
        boolean disordered = false;
        long idDisorderedPacket = 0L;
        long start = System.currentTimeMillis();
        long i = 0;
        while (true) {
            /*
             * i = (i)%animationChars.length;
             * System.out.print("Working on "+ inputFile+" "+ animationChars[i] +"\r");
             */
            try {
                BasicPacketInfo basicPacket = packetReader.nextPacket();
                nTotal++;
                if (basicPacket != null) {
                    basicPacket.setCount(i);
                    //
                    // Check that pcap file isn't disordered to make sure to obtain consistent
                    // network flows.
                    //
                    currentTimestamp = basicPacket.getTimeStamp();
                    if (!(disordered) && (previousTimestamp > currentTimestamp)) {
                        idDisorderedPacket = basicPacket.getId(); // save ID of the first disordered packet.
                        disordered = true;

                        // The pcap file is disordered thus show the warning to user
                        System.out.println(DividingLine);
                        System.out.println(
                                "/!\\ The pcap file contains disordered packets ! The network flows may be incorrect.");
                        System.out.println(String.format("The packet with ID %d is the first disordered one.",
                                idDisorderedPacket));
                        System.out.println("Please order your pcap file and run the tool again.");
                        System.out.println(DividingLine);

                    } else {
                        previousTimestamp = currentTimestamp;
                    }
                    flowGen.addPacket(basicPacket);
                    nValid++;
                } else {
                    nDiscarded++;
                }
            } catch (PcapClosedException e) {
                break;
            }
            i++;
        }

        flowGen.dumpLabeledCurrentFlow(saveFileFullPath.getPath(), FlowFeature.getHeader());

        long lines = SwingUtils.countLines(saveFileFullPath.getPath());

        System.out.println(String.format("%s is done. total %d flows ", fileName, lines));
        System.out.println(String.format("Packet stats: Total=%d,Valid=%d,Discarded=%d", nTotal, nValid, nDiscarded));
        System.out.println(DividingLine);
    }

    static class FlowListener implements FlowGenListener {

        private String fileName;

        private String outPath;

        private long cnt;

        public FlowListener(String fileName, String outPath) {
            this.fileName = fileName;
            this.outPath = outPath;
        }

        @Override
        public void onFlowGenerated(BasicFlow flow) {

            String flowDump = flow.dumpFlowBasedFeaturesEx();
            List<String> flowStringList = new ArrayList<>();
            flowStringList.add(flowDump);

            StringBuilder indexDump = new StringBuilder();
            for (BasicPacketInfo packet : flow.getForward()) {
                indexDump.append(packet.getCount()).append(",");
            }

            for (BasicPacketInfo packet : flow.getBackward()) {
                indexDump.append(packet.getCount()).append(",");
            }

            List<String> indexStringList = new ArrayList<>();
            indexStringList.add(indexDump.toString());

            InsertCsvRow.insert(FlowFeature.getHeader(), flowStringList, outPath, fileName + FlowMgr.FLOW_SUFFIX,
                    indexStringList);

            cnt++;

            String console = String.format("%s -> %d flows \r", fileName, cnt);

            System.out.print(console);
        }
    }

}
