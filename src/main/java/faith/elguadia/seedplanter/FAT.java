package faith.elguadia.seedplanter;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
    Resources used to make the FAT driver:
        http://www.tavi.co.uk/phobos/fat.html
        http://www.dfists.ua.es/~gil/FAT12Description.pdf
        http://fileadmin.cs.lth.se/cs/Education/EDA385/HT09/student_doc/FinalReports/FAT12_overview.pdf
        https://staff.washington.edu/dittrich/misc/fatgen103.pdf
        http://read.pudn.com/downloads77/ebook/294884/FAT32%20Spec%20%28SDA%20Contribution%29.pdf
        https://en.wikipedia.org/wiki/File_Allocation_Table
        https://en.wikipedia.org/wiki/Design_of_the_FAT_file_system
 */

class FATC {
    //I use the notation used by https://staff.washington.edu/dittrich/misc/fatgen103.pdf, see page 9
    static int BPB_BytsPerSec;  //This is nearly always 512
    static int BPB_SecPerClus;  //How many sectors make up a cluster
    static int BPB_RsvdSecCnt;  //This is nearly always 1, since you have a single sector (the BPB sector)
    static int BPB_NumFATs;     //This is nearly always 2
    static int BPB_RootEntCnt;  //How many entries does the root have
    static int BPB_FATSz16;     //How many sectors does a FAT table take up

    static int FATableStartSector;
    static int RootStartSector;
    static int DataStartSector;
}

public class FAT {
    private byte[] FatIMG;

    FAT(byte[] image) {
        FatIMG = image;
        readStructure();
    }

    private void readStructure() {
        //========== Set up FAT constants ==========
        ByteBuffer buffer = ByteBuffer.allocate(25).order(ByteOrder.LITTLE_ENDIAN).put(FatIMG, 0x00, 25); //25 is just a number that happens to contain the entire BPB

        FATC.BPB_BytsPerSec = buffer.getShort(11);
        FATC.BPB_SecPerClus = buffer.get(13);
        FATC.BPB_RsvdSecCnt = buffer.getShort(14);
        FATC.BPB_NumFATs = buffer.get(16);
        FATC.BPB_RootEntCnt = buffer.getShort(17);
        FATC.BPB_FATSz16 = buffer.getShort(22);

        FATC.FATableStartSector = FATC.BPB_RsvdSecCnt;
        FATC.RootStartSector = FATC.FATableStartSector + FATC.BPB_NumFATs * FATC.BPB_FATSz16;
        FATC.DataStartSector = FATC.RootStartSector + ((FATC.BPB_RootEntCnt * 0x20) / FATC.BPB_BytsPerSec);
    }

    private void setFATableEntry(int FATentry, int value) {
        int FATableAddr = FATC.FATableStartSector * FATC.BPB_BytsPerSec;

        int offset;
        if (FATentry == 0 || FATentry == 1)
            offset = 0;
        else
            offset = (FATentry % 2 != 0) ? ((FATentry - 1) / 2) * 0x03 : (FATentry / 2) * 0x03;

        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 4; i++)
            buffer.put(FatIMG[FATableAddr + offset + i]);

        int firstEntry  =  buffer.getInt(0) & 0x00000FFF;
        int secondEntry = (buffer.getInt(0) & 0X00FFF000) >> 12;

        if (FATentry % 2 == 0)
            firstEntry = value;
        else
            secondEntry = value;

        //now we have to rebuild this into an array and insert it back in the image
        int curEntry = firstEntry | (secondEntry << 12) | (buffer.get(3) << 24);
        buffer.putInt(0, curEntry);

        //now let's write back the array into the image
        System.arraycopy(buffer.array(), 0, FatIMG, FATableAddr + offset, 4);
    }

    public void clearRoot() {
        Arrays.fill(FatIMG, 0x200, FatIMG.length, (byte)0x00);
        //The first 2 entries in the FAT table are special, we need to set them
        int FATableStartAddr = FATC.FATableStartSector * FATC.BPB_BytsPerSec;
        FatIMG[FATableStartAddr]   = (byte)0xF8;
        FatIMG[FATableStartAddr+1] = (byte)0xFF;
        FatIMG[FATableStartAddr+2] = (byte)0xFF;
    }

    //Notice how it has "single" in the title
    //it means that you can't call this method after you've called it once, it just won't work
    //if you want, you can clear root first and *then* call this method
    public void copySingleFileToRoot(String filename, byte[] filedata) {
        int RootStartAddr = FATC.RootStartSector * FATC.BPB_BytsPerSec;
        int DataStartAddr = FATC.DataStartSector * FATC.BPB_BytsPerSec;
        //We need to make an entry in the root

        //The first thing is to put the filename and the file attributes (in our case we only put the ARCHIVE attribute)
        byte[] filename_bytes = null; try { filename_bytes = filename.getBytes("US-ASCII"); } catch (Exception e) {} //These pointless exceptions...
        System.arraycopy(filename_bytes, 0, FatIMG, RootStartAddr, 11);
        FatIMG[RootStartAddr + 11] = 0x20;

        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        //Now we put in the starting cluster
        buffer.putShort((short)2);
        System.arraycopy(buffer.array(), 0, FatIMG, RootStartAddr + 0x1A, 2);
        buffer.rewind();
        //And now in goes the filesize
        buffer.putInt(filedata.length);
        System.arraycopy(buffer.array(), 0, FatIMG, RootStartAddr + 0x1C,4);

        //All that's left to do is to set the entries in the FAT table, and copy all of the file's data into the data region

        //Copy the actual data of the file into the data region
        System.arraycopy(filedata, 0, FatIMG, DataStartAddr, filedata.length);
        //And now put incrementing clusters pointing to each successive cluster of our file
        int File_NumOfClusters = filedata.length / (FATC.BPB_SecPerClus * FATC.BPB_BytsPerSec);
        File_NumOfClusters += (filedata.length % (FATC.BPB_SecPerClus * FATC.BPB_BytsPerSec) == 0) ? 0 : 1; //If it doesn't fit entirely in one cluster, you need to add another extra cluster
        for (int i = 0; i < File_NumOfClusters; i++) {
            setFATableEntry(2 + i, 2 + 1 + i);
        }
        setFATableEntry(2 + File_NumOfClusters - 1, 0xFFF); //Set the final entry to 0xFFF to signal the end of a file
    }

    //copy the first FAT table into the second FAT table
    public void copyFATable() {
        int RootStartAddr = FATC.RootStartSector * FATC.BPB_BytsPerSec;
        int FATSzBytes = FATC.BPB_FATSz16 * FATC.BPB_BytsPerSec;
        System.arraycopy(FatIMG, RootStartAddr, FatIMG, (RootStartAddr + FATSzBytes), FATSzBytes);
    }
}
