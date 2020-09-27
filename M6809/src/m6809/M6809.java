/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/*
** Revision history
** V1.0 initial vesion
** V1.1 Add support for NMI, IRQ, and FIRQ, correct clock cycles for indexed
** instructions
*/

package m6809;

import java.io.*;
/**
 *
 * @author daves
 */
public class M6809 {

    /**
     * @param args the command line arguments
     */
    final static String VERSION = "V1.1";
    
    final static int BYTESPERSRECORD = 16;
    
    public static void main(String[] args) {
        // TODO code application logic here
        int icounter;
        int icounter2;
        int junk = 0;
        
        MemoryModule CPUMem = new MemoryModule();
        CPU CPU6809 = new CPU(CPUMem);
        CPU6809.Reset();
        UI gui = new UI(CPU6809, CPUMem);
        gui.FinishUIInit();
        gui.setVisible(true);
        
  
        
        while(true)
        {   
            for(icounter = 0; icounter < CPU6809.GetRealClockDelay(); icounter++)
            {
                for(icounter2 = 0; icounter2 < CPU6809.GetRealClockDelay(); icounter2++)
                {
                    junk++;
                }
            }
            CPU6809.clock();
        }   
        
    }
    
    public static void WriteSRecordFile (FileWriter out, MemoryModule mem, CPU aCPU)
    {
        int iAddress;
        int index;
        SRecord srec = new SRecord();
        srec.Type = 0;
        srec.address = 0;
        srec.data[0] = 0x45; // "E"
        srec.data[1] = 0x54; // "T"
        srec.data[2] = 0x2d; // "-"
        srec.data[3] = 0x33; // "3"
        srec.data[4] = 0x34; // "4"
        srec.data[5] = 0x30; // "0"
        srec.data[6] = 0x30; // "0"
        srec.size = 10;
        srec.dataBytes = 7;
        srec.calcChecksum();
        try {
            out.write(srec.SRecordToString());
        } catch (IOException exc) {
            
        }
        aCPU.Halt(true);
        for(iAddress = 0; iAddress < mem.RAMSIZE; iAddress += BYTESPERSRECORD)
        {
            srec.address = iAddress;
            srec.Type = 1;
            for(index = 0; index < BYTESPERSRECORD; index++)
            {
                srec.data[index] = mem.MemRead(iAddress + index);
            }
            srec.dataBytes = BYTESPERSRECORD;
            srec.size = 19; //2 byte address + 16 data bytes + checksum
            srec.calcChecksum();
            try {
                out.write(srec.SRecordToString());
            }  catch (IOException exc)  {
                
            }
        }
        // write ending S-record
        srec.Type = 9;
        srec.address = 0;
        srec.size = 3;
        srec.dataBytes = 0;
        srec.calcChecksum();
        try {
            out.write(srec.SRecordToString());
        } catch (IOException exc) {
            
        }

        aCPU.Halt(false);
    }
    
    public static int ReadSRecordFile (FileReader in,  MemoryModule mem, CPU aCPU)
    {
        String instring;
        SRecord srec = new SRecord();
        int result;
        int iAddress;
        int index;
        instring = ReadString(in);
        result = srec.ParseFromString(instring); //try starting srecord
        if(result != SRecord.NO_ERROR)
        {
            return (result);
        }
        aCPU.Halt(true);
        //for(index=0;index < mem.RAMSIZE;index++)
        //    mem.MemWrite(index, 0);
        
        if(srec.Type == 1)
        {
            iAddress = srec.address;
            for(index=0;index < srec.dataBytes;index++)
            {
                mem.MemWrite(iAddress++, srec.data[index]);
            }
        }
        else
            iAddress = 0;
        while(iAddress < mem.RAMSIZE)
        {
            instring = ReadString(in);
            if(instring.compareTo("") == 0) // may have reached the end
                break;
            result = srec.ParseFromString(instring); //try starting srecord
            if (result == SRecord.NO_ERROR)
            {
                if(srec.Type == 1)
                {
                    iAddress = srec.address;
                    for(index=0;index < srec.dataBytes;index++)
                    {
                        mem.MemWrite(iAddress++, srec.data[index]);
                    }
                }
                else if(srec.Type == 9) //reached the end
                {
                    break;
                }
            }
            else
                return (result);
        }
        
        aCPU.ResetRequest();
        aCPU.Halt(false);
        return (result);
    }
    
    public static String ReadString (FileReader in)
    {
        String instring = "";
        int inchar;
        do 
        {
            try {
                inchar = in.read();
            } catch (IOException exc) {
                return (instring);
            }
            if(inchar != -1) //EOF reached
                instring = instring + (char)inchar;
            else
                return (instring);
        } while ((inchar != '\n') && (inchar != 0));
        return (instring);
    }
    
}
