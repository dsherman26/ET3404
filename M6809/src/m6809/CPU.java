/*
 * CPU.java
** Implements  functionality of Motorola 6809 processor.
**
** Dave Sherman 03/29/2020
**
** Revisions:
** Add NMI, IRQ, FIRQ functionality
** Add additional clock cycles for indexed instructions
** 
 */
package m6809;

/**
 *
 * @author daves
 */
public class CPU
{

    private enum CommandStates
    {
        COMMAND,
        CLOCKWAIT,
        MORECLOCKS,
    };

    private enum Register
    {
        A,
        B,
        D,
        X,
        Y,
        PC,
        SP,
        UP,
        DP,
        CC,
    };

    private int ACCA;
    private int ACCB;
    private int X;
    private int Y;
    private int SP;
    private int UP;
    private int PC;
    private int DP;
    private boolean C;
    private boolean V;
    private boolean Z;
    private boolean N;
    private boolean I;
    private boolean H;
    private boolean F;
    private boolean E;

    private boolean ResetReq;
    private boolean IRQFlag;
    private boolean NMIFlag;
    private boolean NMIInhibit;
    private boolean FIRQFlag;
    private boolean CWAIFlag;
    private boolean SyncFlag;
    private boolean Halted;
    private boolean debug;
    private int lastLocation; // for instructions that modify a value

    final static int MEMEND = 0xFFFF;
    final static int NUMCOMMANDS = 269; //268 valid opcodes, one invalid
    final static int EXTENDED_OPCODE = 0x10;
    final static int EXTENDED_OPCODE2 = 0x11;
    
    final static int RESETVECTOR = 0xFFFE;
    final static int NMIVECTOR = 0xFFFC;
    final static int SWIVECTOR = 0xFFFA;
    final static int IRQVECTOR = 0xFFF8;
    final static int FIRQVECTOR = 0xFFF6;
    final static int SWI2VECTOR = 0xFFF4;
    final static int SWI3VECTOR = 0xFFF2;

    public static final int MINCLOCKDELAY = 10;
    public static final int MAXCLOCKDELAY = 1000;
    public static final int DEFAULTCLOCKDELAY = 100;

    private CommandStates state;
    private Instruction CPUInstructions[];
    private final MemoryModule mem;
    private int clockstep;
    private int moreclocks;
    private Instruction CurrentInstruction;
    private int debugstop = 0x002b;
    private int illegal = 0;

    private int ClockDelay = DEFAULTCLOCKDELAY;

    public void Reset()
    {
        ACCA = 0;
        ACCB = 0;
        X = 0;
        Y = 0;
        SP = 0;
        UP = 0;
        DP = 0;
        C = false;
        V = false;
        Z = false;
        N = false;
        I = true;
        H = false;
        F = true;
        E = false;

        PC = ((mem.MemRead(RESETVECTOR) << 8) + mem.MemRead(RESETVECTOR+1));
        clockstep = 0;
        moreclocks =  0;
        state = CommandStates.COMMAND;
        ResetReq = false;
        CWAIFlag = false;
        NMIInhibit = true;
        NMIFlag = false;
        IRQFlag = false;
        FIRQFlag = false;
        SyncFlag = false;
    }
    
    public void ResetRequest()
    {
        ResetReq = true;
    }
    
    public void Halt (boolean bHalt)
    {
        Halted = bHalt;
    }

    public void clock()
    {
        if (ResetReq)
        {
            Reset();
        }
        else if (NMIFlag && !NMIInhibit)
        {
            NMI();
        }
        else if(IRQFlag)
        {
            IRQ();
        }
        else if(FIRQFlag)
        {
            FIRQ();
        }
        else if(!CWAIFlag && !SyncFlag && !Halted)
        {
            switch (state) {
                case COMMAND:
                    if (PC == debugstop)
                    {
                        debug = true;
                    }
                    CurrentInstruction = InstructionLookup(mem.MemRead(PC));
                    clockstep = CurrentInstruction.cycles - 1;
                    state = CommandStates.CLOCKWAIT;
                    PC++;
                    break;
                case CLOCKWAIT:
                    if ((clockstep == 0) || (--clockstep == 0))
                    {
                        DoInstruction(CurrentInstruction.ID, CurrentInstruction.mode);
                        if(moreclocks > 0)
                            state = CommandStates.MORECLOCKS;
                        else
                            state = CommandStates.COMMAND;
                    }
                    break;
                case MORECLOCKS:
                    if(--moreclocks == 0)
                        state = CommandStates.COMMAND;
                    break;
            }
        }
    }

    public void IRQReq()
    {
        if(!I)
            IRQFlag = true;
    }
    
    public void FIRQReq()
    {
        if(!F)
            FIRQFlag = true;
    }
    
    public void NMIReq()
    {
        NMIFlag = true;
    }
    
    public CPU(MemoryModule mem)
    {
        this.mem = mem;
        state = CommandStates.COMMAND;
        InitInstructions();
    }

    private void InitInstructions()
    {
        int icounter = 0;
        CPUInstructions = new Instruction[NUMCOMMANDS];
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ABX, 0x3A, Instruction.AddressMode.INHERENT, 3, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCA, 0x89, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCA, 0x99, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCA, 0xA9, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCA, 0xB9, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCB, 0xC9, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCB, 0xD9, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCB, 0xE9, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADCB, 0xF9, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDA, 0x8B, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDA, 0x9B, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDA, 0xAB, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDA, 0xBB, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDB, 0xCB, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDB, 0xDB, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDB, 0xEB, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDB, 0xFB, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDD, 0xC3, Instruction.AddressMode.IMMEDIATE, 4, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDD, 0xD3, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDD, 0xE3, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ADDD, 0xF3, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDA, 0x84, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDA, 0x94, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDA, 0xA4, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDA, 0xB4, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDB, 0xC4, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDB, 0xD4, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDB, 0xE4, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDB, 0xF4, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ANDCC, 0x1C, Instruction.AddressMode.IMMEDIATE, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASL, 0x08, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASL, 0x68, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASL, 0x78, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASLA, 0x48, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASLB, 0x58, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASR, 0x07, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASR, 0x67, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASR, 0x77, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASRA, 0x47, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ASRB, 0x57, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BCC, 0x24, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBCC, 0x1024, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BCS, 0x25, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBCS, 0x1025, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BEQ, 0x27, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBEQ, 0x1027, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BGE, 0x2C, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBGE, 0x102C, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BGT, 0x2E, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBGT, 0x102E, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BHI, 0x22, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBHI, 0x1022, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITA, 0x85, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITA, 0x95, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITA, 0xA5, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITA, 0xB5, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITB, 0xC5, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITB, 0xD5, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITB, 0xE5, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BITB, 0xF5, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BLE, 0x2F, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBLE, 0x102F, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BLS, 0x23, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBLS, 0x1023, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BLT, 0x2D, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBLT, 0x102D, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BMI, 0x2B, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBMI, 0x102B, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BNE, 0x26, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBNE, 0x1026, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BPL, 0x2A, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBPL, 0x102A, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BRA, 0x20, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBRA, 0x16, Instruction.AddressMode.INHERENT, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BRN, 0x21, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBRN, 0x1021, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BSR, 0x8D, Instruction.AddressMode.INHERENT, 7, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBSR, 0x17, Instruction.AddressMode.INHERENT, 9, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BVC, 0x28, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBVC, 0x1028, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.BVS, 0x29, Instruction.AddressMode.INHERENT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LBVS, 0x1029, Instruction.AddressMode.INHERENT, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CLR, 0x0F, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CLR, 0x6F, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CLR, 0x7F, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CLRA, 0x4F, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CLRB, 0x5F, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPA, 0x81, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPA, 0x91, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPA, 0xA1, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPA, 0xB1, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPB, 0xC1, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPB, 0xD1, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPB, 0xE1, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPB, 0xF1, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPD, 0x1083, Instruction.AddressMode.IMMEDIATE, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPD, 0x1093, Instruction.AddressMode.DIRECT, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPD, 0x10A3, Instruction.AddressMode.INDEXED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPD, 0x10B3, Instruction.AddressMode.EXTENDED, 8, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPS, 0x118C, Instruction.AddressMode.IMMEDIATE, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPS, 0x119C, Instruction.AddressMode.DIRECT, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPS, 0x11AC, Instruction.AddressMode.INDEXED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPS, 0x11BC, Instruction.AddressMode.EXTENDED, 8, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPU, 0x1183, Instruction.AddressMode.IMMEDIATE, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPU, 0x1193, Instruction.AddressMode.DIRECT, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPU, 0x11A3, Instruction.AddressMode.INDEXED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPU, 0x11B3, Instruction.AddressMode.EXTENDED, 8, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPX, 0x8C, Instruction.AddressMode.IMMEDIATE, 4, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPX, 0x9C, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPX, 0xAC, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPX, 0xBC, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPY, 0x108C, Instruction.AddressMode.IMMEDIATE, 5, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPY, 0x109C, Instruction.AddressMode.DIRECT, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPY, 0x10AC, Instruction.AddressMode.INDEXED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CMPY, 0x10BC, Instruction.AddressMode.EXTENDED, 8, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.COM, 0x03, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.COM, 0x63, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.COM, 0x73, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.COMA, 0x43, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.COMB, 0x53, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.CWAI, 0x3C, Instruction.AddressMode.IMMEDIATE, 20, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.DAA, 0x19, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.DEC, 0x0A, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.DEC, 0x6A, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.DEC, 0x7A, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.DECA, 0x4A, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.DECB, 0x5A, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORA, 0x88, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORA, 0x98, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORA, 0xA8, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORA, 0xB8, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORB, 0xC8, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORB, 0xD8, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORB, 0xE8, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EORB, 0xF8, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.EXG, 0x1E, Instruction.AddressMode.IMMEDIATE, 8, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.INC, 0x0C, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.INC, 0x6C, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.INC, 0x7C, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.INCA, 0x4C, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.INCB, 0x5C, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.JMP, 0x0E, Instruction.AddressMode.DIRECT, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.JMP, 0x6E, Instruction.AddressMode.INDEXED, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.JMP, 0x7E, Instruction.AddressMode.EXTENDED, 4, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.JSR, 0x9D, Instruction.AddressMode.DIRECT, 7, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.JSR, 0xAD, Instruction.AddressMode.INDEXED, 7, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.JSR, 0xBD, Instruction.AddressMode.EXTENDED, 8, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDA, 0x86, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDA, 0x96, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDA, 0xA6, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDA, 0xB6, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDB, 0xC6, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDB, 0xD6, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDB, 0xE6, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDB, 0xF6, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDD, 0xCC, Instruction.AddressMode.IMMEDIATE, 3, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDD, 0xDC, Instruction.AddressMode.DIRECT, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDD, 0xEC, Instruction.AddressMode.INDEXED, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDD, 0xFC, Instruction.AddressMode.EXTENDED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDS, 0x10CE, Instruction.AddressMode.IMMEDIATE, 4, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDS, 0x10DE, Instruction.AddressMode.DIRECT, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDS, 0x10EE, Instruction.AddressMode.INDEXED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDS, 0x10FE, Instruction.AddressMode.EXTENDED, 7, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDU, 0xCE, Instruction.AddressMode.IMMEDIATE, 3, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDU, 0xDE, Instruction.AddressMode.DIRECT, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDU, 0xEE, Instruction.AddressMode.INDEXED, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDU, 0xFE, Instruction.AddressMode.EXTENDED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDX, 0x8E, Instruction.AddressMode.IMMEDIATE, 3, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDX, 0x9E, Instruction.AddressMode.DIRECT, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDX, 0xAE, Instruction.AddressMode.INDEXED, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDX, 0xBE, Instruction.AddressMode.EXTENDED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDY, 0x108E, Instruction.AddressMode.IMMEDIATE, 4, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDY, 0x109E, Instruction.AddressMode.DIRECT, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDY, 0x10AE, Instruction.AddressMode.INDEXED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LDY, 0x10BE, Instruction.AddressMode.EXTENDED, 7, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LEAS, 0x32, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LEAU, 0x33, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LEAX, 0x30, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LEAY, 0x31, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LSR, 0x04, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LSR, 0x64, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LSR, 0x74, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LSRA, 0x44, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.LSRB, 0x54, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.MUL, 0x3D, Instruction.AddressMode.INHERENT, 11, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.NEG, 0x00, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.NEG, 0x60, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.NEG, 0x70, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.NEGA, 0x40, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.NEGB, 0x50, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.NOP, 0x12, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORA, 0x8A, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORA, 0x9A, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORA, 0xAA, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORA, 0xBA, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORB, 0xCA, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORB, 0xDA, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORB, 0xEA, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORB, 0xFA, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ORCC, 0x1A, Instruction.AddressMode.IMMEDIATE, 3, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.PSHS, 0x34, Instruction.AddressMode.IMMEDIATE, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.PSHU, 0x36, Instruction.AddressMode.IMMEDIATE, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.PULS, 0x35, Instruction.AddressMode.IMMEDIATE, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.PULU, 0x37, Instruction.AddressMode.IMMEDIATE, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROL, 0x09, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROL, 0x69, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROL, 0x79, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROLA, 0x49, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROLB, 0x59, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROR, 0x06, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROR, 0x66, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.ROR, 0x76, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.RORA, 0x46, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.RORB, 0x56, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.RTI, 0x3B, Instruction.AddressMode.INHERENT, 6, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.RTS, 0x39, Instruction.AddressMode.INHERENT, 5, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCA, 0x82, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCA, 0x92, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCA, 0xA2, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCA, 0xB2, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCB, 0xC2, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCB, 0xD2, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCB, 0xE2, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SBCB, 0xF2, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SEX, 0x1D, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STA, 0x97, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STA, 0xA7, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STA, 0xB7, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STB, 0xD7, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STB, 0xE7, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STB, 0xF7, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STD, 0xDD, Instruction.AddressMode.DIRECT, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STD, 0xED, Instruction.AddressMode.INDEXED, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STD, 0xFD, Instruction.AddressMode.EXTENDED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STS, 0x10DF, Instruction.AddressMode.DIRECT, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STS, 0x10EF, Instruction.AddressMode.INDEXED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STS, 0x10FF, Instruction.AddressMode.EXTENDED, 7, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STU, 0xDF, Instruction.AddressMode.DIRECT, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STU, 0xEF, Instruction.AddressMode.INDEXED, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STU, 0xFF, Instruction.AddressMode.EXTENDED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STX, 0x9F, Instruction.AddressMode.DIRECT, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STX, 0xAF, Instruction.AddressMode.INDEXED, 5, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STX, 0xBF, Instruction.AddressMode.EXTENDED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STY, 0x109F, Instruction.AddressMode.DIRECT, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STY, 0x10AF, Instruction.AddressMode.INDEXED, 6, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.STY, 0x10BF, Instruction.AddressMode.EXTENDED, 7, 4);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBA, 0x80, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBA, 0x90, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBA, 0xA0, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBA, 0xB0, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBB, 0xC0, Instruction.AddressMode.IMMEDIATE, 2, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBB, 0xE0, Instruction.AddressMode.DIRECT, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBB, 0xE0, Instruction.AddressMode.INDEXED, 4, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBB, 0xF0, Instruction.AddressMode.EXTENDED, 5, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBD, 0x83, Instruction.AddressMode.IMMEDIATE, 4, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBD, 0x93, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBD, 0xA3, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SUBD, 0xB3, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SWI, 0x3F, Instruction.AddressMode.INHERENT, 19, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SWI2, 0x103F, Instruction.AddressMode.INHERENT, 20, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SWI3, 0x113F, Instruction.AddressMode.INHERENT, 20, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.SYNC, 0x13, Instruction.AddressMode.INHERENT, 4, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.TFR, 0x1F, Instruction.AddressMode.IMMEDIATE, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.TST, 0x0D, Instruction.AddressMode.DIRECT, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.TST, 0x6D, Instruction.AddressMode.INDEXED, 6, 2);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.TST, 0x7D, Instruction.AddressMode.EXTENDED, 7, 3);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.TSTA, 0x4D, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.TSTB, 0x5D, Instruction.AddressMode.INHERENT, 2, 1);
        CPUInstructions[icounter++] = new Instruction(Instruction.CommandID.INVALID, 0x0, Instruction.AddressMode.INHERENT, 1, 1);
    }

    Instruction InstructionLookup(int opcode)
    {
        if ((opcode == EXTENDED_OPCODE) || (opcode == EXTENDED_OPCODE2))
        {
            opcode = ((opcode << 8) + mem.MemRead(PC+1));
            PC++;
        }
        Instruction instruction = CPUInstructions[0];
        int icounter;
        for (icounter = 0; icounter < NUMCOMMANDS; icounter++)
        {
            instruction = CPUInstructions[icounter];
            if (instruction.opcode == opcode)
            {
                break;
            }
        }
        return (instruction);
    }

    private void DoInstruction(Instruction.CommandID ID, Instruction.AddressMode mode)
    {
        switch (ID) {
            case ABX:
                ABX();
                break;
            case ADCA:
                ADD(mode, Register.A, true);
                break;
            case ADCB:
                ADD(mode, Register.B, true);
                break;
            case ADDA:
                ADD(mode, Register.A, false);
                break;
            case ADDB:
                ADD(mode, Register.B, false);
                break;
            case ADDD:
                ADD16(mode, Register.D, false);
                break;
            case ANDA:
                AND(mode, Register.A);
                break;
            case ANDB:
                AND(mode, Register.B);
                break;
            case ANDCC:
                ANDCC();
                break;
            case ASL:
                ASL(mode);
                break;
            case ASLA:
                ASLReg(Register.A);
                break;
            case ASLB:
                ASLReg(Register.B);
                break;
            case ASR:
                ASR(mode);
                break;
            case ASRA:
                ASRReg(Register.A);
                break;
            case ASRB:
                ASRReg(Register.B);
                break;
            case BCC:
                if(!C)
                    branch();
                else
                    PC++;
                break;
            case LBCC:
                if(!C)
                    longbranch();
                else
                    PC+=2;
                break;
            case BCS:
                if(C)
                    branch();
                else
                    PC++;
                break;
            case LBCS:
                if(C)
                    longbranch();
                else
                    PC+=2;
                break;
            case BEQ:
                if(Z)
                    branch();
                else
                    PC++;
                break;
            case LBEQ:
                if(Z)
                    longbranch();
                else
                    PC+=2;
                break;
            case BGE:
                if((N && V) || (!N && !V))
                    branch();
                else
                    PC++;
                break;
            case LBGE:
                if((N && V) || (!N && !V))
                    longbranch();
                else
                    PC+=2;
                break;
            case BGT:
                if(!Z && ((N && V) || (!N && !V)))
                    branch();
                else
                    PC++;
                break;
            case LBGT:
                if(!Z && ((N && V) || (!N && !V)))
                    longbranch();
                else
                    PC+=2;
                break;
            case BHI:
                if(!C && !Z)
                    branch();
                else
                    PC++;
                break;
            case LBHI:
                if(!C && !Z)
                    longbranch();
                else
                    PC+=2;
                break;
            case BITA:
                BIT(mode, Register.A);
                break;
            case BITB:
                BIT(mode, Register.B);
                break;
            case BLE:
                if(Z || ((N && !V) || (!N && V)))
                    branch();
                else
                    PC++;
                break;
            case LBLE:
                if(Z || ((N && !V) || (!N && V)))
                    longbranch();
                else
                    PC+=2;
                break;
            case BLS:
                if(C || Z)
                    branch();
                else
                    PC++;
                break;
            case LBLS:
                if(C || Z)
                    longbranch();
                else
                    PC+=2;
                break;
            case BLT:
                if((N && !V) || (!N && V))
                    branch();
                else
                    PC++;
                break;
            case LBLT:
                if((N && !V) || (!N && V))
                    longbranch();
                else
                    PC+=2;
                break;
            case BMI:
                if (N)
                    branch();
                else
                    PC++;
                break;
            case LBMI:
                if (N)
                    longbranch();
                else
                    PC+=2;
                break;
            case BNE:
                if (!Z)
                    branch();
                else
                    PC++;
                break;
            case BPL:
                if (!N)
                    branch();
                else
                    PC++;
                break;
            case LBPL:
                if (!N)
                    longbranch();
                else
                    PC+=2;
                break;
            case BRA:
                branch();
                break;
            case LBRA:
                longbranch();
                break;
            case BRN:
                PC++;
                break;
            case LBRN:
                PC+=2;
                break;
            case BSR:
                push16(Register.SP, PC + 1);
                    branch();
                break;
            case LBSR:
                push16(Register.SP, PC + 2);
                    longbranch();
                break;
            case BVC:
                if(!V)
                    branch();
                else
                    PC++;
                break;
            case LBVC:
                if(!V)
                    longbranch();
                else
                    PC+=2;
                break;
            case BVS:
                if(V)
                    branch();
                else
                    PC++;
                break;
            case LBVS:
                if(V)
                    longbranch();
                else
                    PC+=2;
                break;
            case CLR:
                CLR(mode);
                break;
            case CLRA:
                ACCA = 0;
                Z = true;
                N = false;
                V = false;
                C = false;
                break;
            case CLRB:
                ACCB = 0;
                Z = true;
                N = false;
                V = false;
                C = false;
                break;
            case CMPA:
                CMP(mode, Register.A);
                break;
            case CMPB:
                CMP(mode, Register.B);
                break;
            case CMPD:
                CMP16(mode, Register.D);
                break;
            case CMPS:
                CMP16(mode, Register.SP);
                break;
            case CMPU:
                CMP16(mode, Register.UP);
                break;
            case CMPX:
                CMP16(mode, Register.X);
                break;
            case CMPY:
                CMP16(mode, Register.Y);
                break;
            case COM:
                COM(mode);
                break;
            case COMA:
                COMReg(Register.A);
                break;
            case COMB:
                COMReg(Register.B);
                break;
            case CWAI:
                CWAI();
                break;
            case DAA:
                DAA();
                break;
            case DEC:
                DEC(mode);
                break;
            case DECA:
                DECReg(Register.A);
                break;
            case DECB:
                DECReg(Register.B);
                break;
            case EORA:
                EOR(mode, Register.A);
                break;
            case EORB:
                EOR(mode, Register.B);
                break;
            case EXG:
                EXG();
                break;
            case INC:
                INC(mode);
                break;
            case INCA:
                INCReg(Register.A);
                break;
            case INCB:
                INCReg(Register.B);
                break;
            case JMP:
                JMP(mode);
                break;
            case JSR:
                JSR(mode);
                break;
            case LDA:
                LD(mode, Register.A);
                break;
            case LDB:
                LD(mode, Register.B);
                break;
            case LDD:
                LD(mode, Register.D);
                break;
            case LDS:
                LD(mode, Register.SP);
                break;
            case LDU:
                LD(mode, Register.UP);
                break;
            case LDX:
                LD(mode, Register.X);
                break;
            case LDY:
                LD(mode, Register.Y);
                break;
            case LEAX:
                LEA(Register.X);
                break;
            case LEAY:
                LEA(Register.Y);
                break;
            case LEAS:
                LEA(Register.SP);
                break;
            case LEAU:
                LEA(Register.UP);
                break;
            //case LSL:
            //    LSL(mode);
            //    break;
            //case LSLA:
            //    LSLReg(Register.A);
            //    break;
            //case LSLB:
            //    LSLReg(Register.B);
            //    break;
            case LSR:
                LSR(mode);
                break;
            case LSRA:
                LSRReg(Register.A);
                break;
            case LSRB:
                LSRReg(Register.B);
                break;
            case MUL:
                MUL();
                break;
            case NEG:
                NEG(mode);
                break;
            case NEGA:
                NEGReg(Register.A);
                break;
            case NEGB:
                NEGReg(Register.B);
                break;
            case NOP:
                break;
            case ORA:
                OR(mode, Register.A);
                break;
            case ORB:
                OR(mode, Register.B);
                break;
            case ORCC:
                ORCC();
                break;
            case PSHS:
                PUSH(Register.SP);
                break;
            case PSHU:
                PUSH(Register.UP);
                break;
            case PULS:
                PULL(Register.SP);
                break;
            case PULU:
                PULL(Register.UP);
                break;
            case ROL:
                ROL(mode);
                break;
            case ROLA:
                ROLReg(Register.A);
                break;
            case ROLB:
                ROLReg(Register.B);
                break;
            case ROR:
                ROR(mode);
                break;
            case RORA:
                RORReg(Register.A);
                break;
            case RORB:
                RORReg(Register.B);
                break;
            case RTI:
                RTI();
                break;
            case RTS:
                RTS();
                break;
            case SBCA:
                SBC(mode, Register.A);
                break;
            case SBCB:
                SBC(mode, Register.B);
                break;
            case SEX:
                SEX();
                break;
            case STA:
                ST(mode, Register.A);
                break;
            case STB:
                ST(mode, Register.B);
                break;
            case STD:
                ST(mode, Register.D);
                break;
            case STS:
                ST(mode, Register.SP);
                break;
            case STU:
                ST(mode, Register.UP);
                break;
            case STX:
                ST(mode, Register.X);
                break;
            case STY:
                ST(mode, Register.Y);
                break;
            case SUBA:
                SUB(mode, Register.A);
                break;
            case SUBB:
                SUB(mode, Register.B);
                break;
            case SUBD:
                SUB16(mode, Register.D);
                break;
            case SWI:
                SWI();
                break;
            case SWI2:
                SWI2();
                break;
            case SWI3:
                SWI3();
                break;
            case SYNC:
                SYNC();
                break;
            case TFR:
                TFR();
                break;
            case TST:
                TST(mode);
                break;
            case TSTA:
                TSTReg(Register.A);
                break;
            case TSTB:
                TSTReg(Register.B);
                break;
            case INVALID:
                illegal++;
                break;
        }
    }

    private void ABX()
    {
        X = X + ACCB;
    }

    /*
**      ADD - implment ADD and ADC commands.
     */
    private void ADD(Instruction.AddressMode mode, Register reg, boolean bCarry)
    {
        int value1, value2, result;
        value1 = GetReg(reg);
        value2 = GetArgument(mode);

        result = (value1 + value2 + (C && bCarry ? 1 : 0)) & 0xFF;
        SetConditionAdd(value1, value2, result);
        StoreReg(reg, result);
    }

    private void ADD16(Instruction.AddressMode mode, Register reg, boolean bCarry)
    {
        int value1, value2, result;
        value1 = GetReg(reg);
        value2 = GetArgument16(mode);
        result = (value1 + value2 + (C && bCarry ? 1 : 0)) & 0xFFFF;
        N = BitTest(result, 16);
        Z = (result == 0);
        V = (BitTest(value1, 15) && BitTest(value2, 15) && !BitTest(result, 15))
                || (!BitTest(value1, 15) && !BitTest(value2, 15) && BitTest(result, 15));
        C = (BitTest(value1, 15) && BitTest(value2, 15))
                || (BitTest(value1, 15) && !BitTest(result, 15))
                || (!BitTest(result, 15) && BitTest(value2, 15));
        StoreReg(reg, result);
    }

    private void AND(Instruction.AddressMode mode, Register reg)
    {
        int value1, value2;
        value1 = GetReg(reg);
        value2 = GetArgument(mode);
        value1 = value1 & value2;
        SetConditionLoad(value1);
        StoreReg(reg, value1);
    }

    private void ANDCC()
    {
        int value = ImmediateValue(1);
        int value2 = GetConditionCode();
        int result = value & value2;
        SetConditionCode(result);
    }
    
    private void ASL (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        C = BitTest(value,7);
        value <<= 1;
        value &= 0xFF;
        N = BitTest(value,7);
        Z = (value == 0);
        V = (N ^ C);
        StoreValue(-1, value);
    }
    
    private void ASLReg(Register reg)
    {
        int value = GetReg(reg);
        C = BitTest(value,7);
        value <<= 1;
        value &= 0xFF;
        N = BitTest(value,7);
        Z = (value == 0);
        V = ((N && !C) || (!N && C));
        StoreReg(reg, value);
    }
    
    private void ASR (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        boolean B7 = BitTest(value, 7);
        C = BitTest(value,0);
        value >>= 1;
        if(B7)
            value |= (1<<7);
        N = BitTest(value,7);
        Z = (value == 0);
        StoreValue(-1, value);
    }
    
    private void ASRReg (Register reg)
    {
        int value = GetReg(reg);
        boolean B7 = BitTest(value, 7);
        C = BitTest(value,0);
        value >>= 1;
        if(B7)
            value |= (1<<7);
        N = BitTest(value,7);
        Z = (value == 0);
        StoreReg(reg, value);
    }
    
    private void BIT(Instruction.AddressMode mode, Register reg)
    {
        int result;
        result = GetArgument(mode);
        result = result & GetReg(reg);
        N = (BitTest(result, 7));
        Z = (result == 0);
        V = false;   
    }
    
    private void CLR(Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        StoreValue(-1, 0);
        Z = true;
        N = false;
        V = false;
        C = false;
    }
    
    private void CMP (Instruction.AddressMode mode, Register reg)
    {
        int value1, value2 = GetArgument(mode);
        int result;
            
        if (reg == Register.A)
            value1 = ACCA;
        else
            value1 = ACCB;
        result = subtract8(value1, value2);
        SetConditionSubtract(value1, value2, result);
    }
    
    private void CMP16 (Instruction.AddressMode mode, Register reg) {
        int value1, value2 = GetArgument16(mode);
        int result;
        value1 = GetReg(reg);
        result = subtract16(value1, value2);
        SetConditionSubtract16(value1, value2, result);
    }
    
    private void COM (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        value = ~value & 0xFF;
        N = BitTest(value, 7);
        Z = (value == 0);
        V = false;
        C = true;
        StoreValue(-1, value);
    }
        
    private void COMReg (Register reg)
    {
        int value = GetReg(reg);
        value = ~value & 0xFF;
        N = BitTest(value, 7);
        Z = (value == 0);
        V = false;
        C = true;
        StoreReg(reg, value);
    }

    private void CWAI () 
    {
        int value = mem.MemRead(PC++);
        int value2 = value & GetConditionCode();
        SetConditionCode(value2);
        E = true;
        push16(Register.SP, PC);
        push16(Register.SP, UP);
        push16(Register.SP, Y);
        push16(Register.SP, X);
        push8(Register.SP, DP);
        push8(Register.SP, ACCB);
        push8(Register.SP, ACCA);
        push8(Register.SP, GetConditionCode());
        CWAIFlag = true;
    }
            
    private void DAA ()
    {
        int nibble_hi, nibble_low;
        nibble_hi = ACCA >> 4;
        nibble_low = ACCA & 0xF;
            
        if(!C)
        {
            if ((nibble_hi < 9) && (nibble_low > 9) && !H)
                ACCA += 0x6;
            if ((nibble_hi < 10) && (nibble_low < 4) && H)
                ACCA += 0x6;
            if((nibble_hi > 9) && (nibble_low < 10) && !H)
            {
                ACCA += 0x60;
                C = true;
            }
            if((nibble_hi > 8) && (nibble_low > 9) && !H)
            {
                ACCA += 0x66;
                C = true;
            }
            if((nibble_hi > 9) && (nibble_low  < 4) && H)
            {
                ACCA += 0x66;
                C = true;
            }
        }
        else
        {
            if((nibble_hi < 3) && (nibble_low < 10) && !H)
                ACCA += 0x60;
            if((nibble_hi < 3) && (nibble_low > 9) && !H)
                ACCA += 0x66;
            if((nibble_hi < 4) && (nibble_low < 4) && H)
                ACCA += 0x66;
        }
        N = (BitTest(ACCA, 7));
        Z = (ACCA == 0);
    }
    
    private void DEC (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        value = DEC8 (value);
        N = BitTest(value, 7);
        Z = (value == 0);
        V = (value == 0x7F);
        StoreValue(-1, value);
    }
        
    private void DECReg (Register reg)
    {
        int value = GetReg(reg);
        value = DEC8 (value);
        N = BitTest(value, 7);
        Z = (value == 0);
        V = (value == 0x7F);
        StoreReg(reg, value);
    }       
    
    private int DEC8 (int arg)
    {
        if(arg > 0)
            return (arg - 1);
        else
            return (255);
    }
    
    private void EOR (Instruction.AddressMode mode, Register reg)
    {
        int result = GetArgument(mode);
        result = (result ^ GetReg(reg));
        N = BitTest(result, 7);
        Z = (result == 0);
        V = false;
        StoreReg(reg, result);
    }
  
    private void EXG()
    {
        int iArg = mem.MemRead(PC++);
        int iR1 = iArg >> 4;
        int iR2 = iArg & 0xF;
        int iValue1,iValue2;
        if(BitTest(iR1, 3) == BitTest(iR2, 3)) 
        {
            iValue1 = GetRegByNibble(iR1);
            iValue2 = GetRegByNibble(iR2);
            StoreRegByNibble(iR1, iValue2);
            StoreRegByNibble(iR2, iValue1);
        }
    }
    
    private int GetRegByNibble(int nibble)
    {
        int value;
        switch (nibble)
        {
            case 0: // D
                value = GetReg(Register.D);
            break;
            case 1: // X
                value = GetReg(Register.X);
            break;
            case 2: // Y
                value = GetReg(Register.Y);
            break;
            case 3: // UP
                value = GetReg(Register.UP);
            break;
            case 4: // SP
                value = GetReg(Register.SP);
            break;
            case 5: // PC
                value = GetReg(Register.PC);
            break;
            case 8: // A
                value = GetReg(Register.A);
            break;
            case 9: // B
                value = GetReg(Register.B);
            break;
            case 0xA: // CC
                value = GetReg(Register.CC);
            break;
            case 0xB: // DP
                value = GetReg(Register.DP);
            break;
            default:
                value = 0;
            break;
        }
        return (value);
    }
    
    private void StoreRegByNibble(int nibble, int value)
    {
        switch (nibble)
        {
            case 0: // D
                StoreReg(Register.D, value);
            break;
            case 1: // X
                StoreReg(Register.X, value);
            break;
            case 2: // Y
                StoreReg(Register.Y, value);
            break;
            case 3: // UP
                StoreReg(Register.UP, value);
            break;
            case 4: // SP
                StoreReg(Register.SP, value);
            break;
            case 5: // PC
                StoreReg(Register.PC, value);
            break;
            case 8: // A
                StoreReg(Register.A, value);
            break;
            case 9: // B
                StoreReg(Register.B, value);
            break;
            case 0xA: // CC
                StoreReg(Register.CC, value);
            break;
            case 0xB: // DP
                StoreReg(Register.DP, value);
            break;
        }
    }
    
    private void INC (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        value = INC8 (value);
        N = BitTest(value, 7);
        Z = (value == 0);
        V = (value == 0x80);
        StoreValue(-1, value);
    }
        
    private void INCReg (Register reg)
    {
        int value = GetReg(reg);
        value = INC8 (value);
        N = BitTest(value, 7);
        Z = (value == 0);
        V = (value == 0x80);
        StoreReg(reg, value);
    }
        
    private int INC8 (int arg)
    {
        if(arg < 255)
            return (arg + 1);
        else
            return (0);
    }
    
    private void JMP (Instruction.AddressMode mode)
    {
        int result = GetJump(mode);
        PC = result;
    }
    
    private void JSR (Instruction.AddressMode mode)
    {
        int result = GetJump(mode);
        //note that GetArgument has already incremented PC according to the mode
        push16(Register.SP, PC);
        PC = result;
    }
    
    private void LD(Instruction.AddressMode mode, Register reg)
    {
        int value;
        switch (reg)
        {
            case A:
                value = GetArgument(mode);
                SetConditionLoad(value);
                StoreReg(Register.A, value);
                break;
            case B:
                value = GetArgument(mode);
                SetConditionLoad(value);
                StoreReg(Register.B, value);
                break;
            case D:
                value = GetArgument16(mode);
                SetConditionLoad16(value);
                StoreReg(Register.D, value);
                break;
            case SP:
                value = GetArgument16(mode);
                SetConditionLoad16(value);
                StoreReg(Register.SP, value);
                break;
            case UP:
                value = GetArgument16(mode);
                SetConditionLoad16(value);
                StoreReg(Register.UP, value);
                break;
            case X:
                value = GetArgument16(mode);
                SetConditionLoad16(value);
                StoreReg(Register.X, value);
                break;
            case Y:
                value = GetArgument16(mode);
                SetConditionLoad16(value);
                StoreReg(Register.Y, value);
                break;        
        }
    }

    private void LEA(Register reg)
    {
        int index = IndexedValue(true, true);
        if ((reg == Register.X) || (reg == Register.Y))
            Z = (index == 0);
        StoreReg(reg, index);
    }
    
    private void LSL (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        C = BitTest(value, 7);
        value <<= 1;
        value &= 0xFF;
        N = BitTest(value, 7);
        Z = (value == 0);
        V = (N && !C) || (!N && C);
        StoreValue(-1, value);
    }
    
    private void LSLReg (Register reg)
    {
       int value = GetReg(reg);
       C = BitTest(value, 7);
       value <<= 1;
       value &= 0xFF;
       N = BitTest(value, 7);
       Z = (value == 0);
       V = (N && !C) || (!N && C);
       StoreReg(reg, value);
    }
    
    private void LSR (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        C = BitTest(value, 0);
        N = false;
        value >>= 1;
        Z = (value == 0);
        StoreValue(-1, value);
    }
    
    private void LSRReg (Register reg)
    {
        int value = GetReg(reg);
        C = BitTest(value, 0);
        N = false;
        value >>= 1;
        Z = (value == 0);
        StoreReg(reg, value);
    }
    
    private void MUL ()
    {
        int value1 = GetReg(Register.A);
        int value2 = GetReg(Register.B);
        int result = value1 * value2;
        Z = (result == 0);
        C = BitTest(result, 7);
        StoreReg(Register.D, result);
    }

    private void NEG (Instruction.AddressMode mode)
    {
        int ivalue = GetArgument(mode);
        ivalue = Negate8(ivalue);
        StoreValue(-1, ivalue);
    }
        
    private void NEGReg (Register reg)
    {
        int ivalue = GetReg(reg);
        ivalue = Negate8(ivalue);
        StoreReg(reg, ivalue);
    }
    
    private int Negate8 (int ivalue)
    {
        int result = 0;
        if (ivalue != 0x80 && ivalue != 0x0)
        {
            if (!BitTest(ivalue, 7))
                result = 256 - ivalue;
            else
                result = (~ivalue & 0xFF) + 1;
        }
        C = (ivalue != 0);
        V = (ivalue == 0x80);
        N = BitTest(result,7);
        Z = (result == 0);
        return (result);
    }
    
    private void OR (Instruction.AddressMode mode, Register reg)
    {
        int value1 = GetReg(reg);
        int value2 = GetArgument(mode);
        int result = value1 | value2;
        N = BitTest(result, 7);
        Z = (result == 0);
        V = false;
    }
    
    private void ORCC ()
    {
        int value1 = GetReg(Register.CC);
        int value2 = mem.MemRead(PC++);
        int result = value1 | value2;
        StoreReg(Register.CC, result);
    }
    
    private void PUSH (Register reg)
    {
        int arg = mem.MemRead(PC++);
        if(BitTest(arg, 7)) // PC
        {
            push16(reg, PC);
        }
        if(BitTest(arg, 6)) // SP or UP
        {
            if(reg == Register.SP)
            {
                push16(reg, UP);
            }
            else
            {
                push16(reg, SP);
            }       
        }
        if(BitTest(arg, 5)) // Y
        {
            push16(reg, Y);
        }
        if(BitTest(arg, 4)) // X
        {
            push16(reg, X);
        }
        if(BitTest(arg, 3)) // DP
        {
            push8(reg, DP);
        }
        if(BitTest(arg, 2)) // ACCB
        {
            push8(reg, ACCB);
        }
        if(BitTest(arg, 1)) // ACCA
        {
            push8(reg, ACCA);
        }
        if(BitTest(arg, 0)) // CC
        {
            push8(reg, GetConditionCode());
        }
    }
    
    private void PULL (Register reg)
    {
        int arg = mem.MemRead(PC++);
        if(BitTest(arg, 0)) // CC
        {
            SetConditionCode(pull8(reg));
        }
        if(BitTest(arg, 1)) // ACCA
        {
            ACCA = pull8(reg);
        }
        if(BitTest(arg, 2)) // ACCB
        {
            ACCB = pull8(reg);
        }
        if(BitTest(arg, 3)) // DP
        {
            DP = pull8(reg);
        }
        if(BitTest(arg, 4)) // X
        {
            X = pull16(reg);
        }
        if(BitTest(arg, 5)) // Y
        {
            Y = pull16(reg);
        }
        if(BitTest(arg, 6)) // SP or UP
        {
            if(reg == Register.SP)
            {
                UP = pull16(reg);
            }
            else
            {
                SP = pull16(reg);
            }
        }
        if(BitTest(arg, 7)) // PC
        {
            PC = pull16(reg);
        }
    }
    
    private void ROL (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        boolean bTemp = BitTest(value, 7);
        int result = ((value << 1) & 0xFF) + (C ? 1 : 0);
        C = bTemp;
        N = BitTest(result, 7);
        V = (N && !C) || (!N && C);
        Z = (result == 0);
        StoreValue(-1, result);
    }
    
    private void ROLReg(Register reg)
    {
        int value = GetReg(reg);
        boolean bTemp = BitTest(value, 7);
        int result = ((value << 1) & 0xFF) + (C ? 1 : 0);
        C = bTemp;
        N = BitTest(result, 7);
        V = (N && !C) || (!N && C);
        Z = (result == 0);
        StoreReg(reg, result);
    }
    
    private void ROR (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        boolean bTemp = BitTest(value, 0);
        int result = (value >> 1) + (C ? 0x80 : 0);
        C = bTemp;
        N = BitTest(result, 7);
        V = (N && !C) || (!N && C);
        Z = (result == 0);
        StoreValue(-1, result);
    }
    
    private void RORReg (Register reg)
    {
        int value = GetReg(reg);
        boolean bTemp = BitTest(value, 0);
        int result = (value >> 1) + (C ? 0x80 : 0);
        C = bTemp;
        N = BitTest(result, 7);
        V = (N && !C) || (!N && C);
        Z = (result == 0);
        StoreReg(reg, result);
    }
    
    private void RTI ()
    {
        SetConditionCode(pull8(Register.SP));
        if(E)
        {
            ACCA = pull8(Register.SP);
            ACCB = pull8(Register.SP);
            DP = pull8(Register.SP);
            X = pull16(Register.SP);
            Y = pull16(Register.SP);
            UP = pull16(Register.SP);
            PC = pull16(Register.SP); 
        }
        else
        {
            PC = pull16(Register.SP);
        }
    }
    
    private void RTS ()
    {
        PC = pull16(Register.SP);
    }
    
    private void SBC (Instruction.AddressMode mode, Register reg)
    {
        int ivalue1 = GetReg(reg);
        int ioldValue = GetArgument(mode);
        int ivalue2 = ioldValue + (C ? 1 : 0);
        int result = subtract8(ivalue1, ivalue2);
        SetConditionSubtract(ivalue1, ioldValue, result);
        StoreReg(reg, result);
    }
    
    private void SEX ()
    {
       if (BitTest(ACCB, 7))
       {
           ACCA = 0xFF;
       }
       else
       {
           ACCA = 0;
       }
       N = BitTest(ACCA, 7);
       Z = (GetReg(Register.D) == 0);
    }
    
    private void ST (Instruction.AddressMode mode, Register reg)
    {
        int value = GetReg(reg);
        switch (reg)
        {
            case A:
            case B:
                ST8(mode, value);
                break;
            case D:
            case SP:
            case UP:
            case X:
            case Y:
                ST16(mode, value);
                break;
        }
    }
    
    private void ST8 (Instruction.AddressMode mode, int value)
    {
        int index = 0;
        switch (mode)
        {
            case DIRECT:
                index = DirectAddress();
                break;
            case EXTENDED:
                index = ExtendedAddress();
                break;
            case INDEXED:
                index = IndexedValue(true,true);
                break;
        }
        mem.MemWrite(index, value);
        N = (BitTest(value, 7));
        Z = (value == 0);
        V = false;
    }
    
    private void ST16 (Instruction.AddressMode mode, int value)
    {
        int index = 0;
        switch (mode)
        {
            case DIRECT:
                index = DirectAddress();
                break;
            case EXTENDED:
                index = ExtendedAddress();
                break;
            case INDEXED:
                index = IndexedValue(true,true);
                break;
        }
        N = BitTest(value, 15);
        Z = (value == 0);
        V = false;
        mem.MemWrite(index, value >> 8);
        mem.MemWrite(index+1, value);
    }
    
    private void SUB (Instruction.AddressMode mode, Register reg)
    {
        int value1 = GetReg(reg);
        int value2 = GetArgument(mode);
        int result =  subtract8(value1, value2);
        SetConditionSubtract(value1, value2, result);
        StoreReg(reg, result);
    }
    
    private void SUB16 (Instruction.AddressMode mode, Register reg)
    {
        int value1 = GetReg(reg);
        int value2 = GetArgument(mode);
        int result =  subtract16(value1, value2);
        SetConditionSubtract16(value1, value2, result);
        StoreReg(reg, result);
    }
    
    private void SWI ()
    {
        E = true;
        push16(Register.SP, PC);
        push16(Register.SP, UP);
        push16(Register.SP, Y);
        push16(Register.SP, X);
        push8(Register.SP, DP);
        push8(Register.SP, ACCB);
        push8(Register.SP, ACCA);
        push8(Register.SP, GetConditionCode());
        I = true;
        F = true;
        PC = ((mem.MemRead(SWIVECTOR) << 8) + mem.MemRead(SWIVECTOR+1));
    }
    
    private void SWI2 ()
    {
        E = true;
        push16(Register.SP, PC);
        push16(Register.SP, UP);
        push16(Register.SP, Y);
        push16(Register.SP, X);
        push8(Register.SP, DP);
        push8(Register.SP, ACCB);
        push8(Register.SP, ACCA);
        push8(Register.SP, GetConditionCode());
        PC = ((mem.MemRead(SWI2VECTOR) << 8) + mem.MemRead(SWI2VECTOR+1));
    }
    
    private void SWI3 ()
    {
        E = true;
        push16(Register.SP, PC);
        push16(Register.SP, UP);
        push16(Register.SP, Y);
        push16(Register.SP, X);
        push8(Register.SP, DP);
        push8(Register.SP, ACCB);
        push8(Register.SP, ACCA);
        push8(Register.SP, GetConditionCode());
        PC = ((mem.MemRead(SWI3VECTOR) << 8) + mem.MemRead(SWI3VECTOR+1));
    }
    
    private void SYNC ()
    {
        if(!I || !F || !NMIInhibit)
            SyncFlag = true;
    }
    
    private void TFR()
    {
        int iArg = mem.MemRead(PC++);
        int iR1 = iArg >> 4;
        int iR2 = iArg & 0xF;
        int iValue;
        if(BitTest(iR1, 3) == BitTest(iR2, 3))
        {
            iValue = GetRegByNibble(iR1);
            StoreRegByNibble(iR2, iValue);
        }
    }
    
    private void TST (Instruction.AddressMode mode)
    {
        int value = GetArgument(mode);
        N = BitTest(value, 7);
        Z = (value == 0);
        V = false;
    }
    
    private void TSTReg (Register reg)
    {
        int value = GetReg(reg);
        N = BitTest(value, 7);
        Z = (value == 0);
        V = false;
    }

    private void SetConditionAdd(int accumulator, int current, int result)
    {
        H = (BitTest(accumulator, 3) && BitTest(current, 3))
                || (BitTest(current, 3) && !BitTest(result, 3))
                || (!BitTest(result, 3) && BitTest(accumulator, 3));
        N = BitTest(result, 7);
        Z = (result == 0);
        V = (BitTest(accumulator, 7) && BitTest(current, 7) && !BitTest(result, 7))
                || (!BitTest(accumulator, 7) && !BitTest(current, 7) && BitTest(result, 7));
        C = (BitTest(accumulator, 7) && BitTest(current, 7))
                || (BitTest(current, 7) && !BitTest(result, 7))
                || (!BitTest(result, 7) && BitTest(accumulator, 7));
    }
    
    private void SetConditionSubtract (int accumulator, int current, int result)
    {
        N = BitTest(result, 7);
        Z = (result == 0);
        V = ((BitTest(accumulator, 7) && !BitTest(current, 7) && !BitTest(result,7)) ||
                (!BitTest(accumulator, 7) && BitTest(current,7) && BitTest(result, 7)));
        C = ((!BitTest(accumulator, 7) && BitTest(current, 7)) ||
                (BitTest(current, 7) && BitTest(result, 7)) ||
                (BitTest(result,7) && !BitTest(accumulator, 7)));
    }
    
    private void SetConditionSubtract16 (int accumulator, int current, int result)
    {
        N = BitTest(result, 15);
        Z = (result == 0);
        V = ((BitTest(accumulator, 15) && !BitTest(current, 15) && !BitTest(result,15)) ||
                (!BitTest(accumulator, 15) && BitTest(current,15) && BitTest(result, 15)));
        C = ((!BitTest(accumulator, 15) && BitTest(current, 15)) ||
                (BitTest(current, 15) && BitTest(result, 15)) ||
                (BitTest(result,15) && !BitTest(accumulator, 15)));
    }
    
    private int subtract8 (int arg1, int arg2)
    {
        if(arg1 >= arg2)
            return (arg1 - arg2);
        else
            return (256 - (arg2 - arg1));
    }
    
    private int subtract16 (int arg1, int arg2)
    {
        if(arg1 >= arg2)
            return (arg1 - arg2);
        else
            return (65536 - (arg2 - arg1));
    }
    
    private void SetConditionLoad(int value)
    {
        N = BitTest(value, 7);
        Z = (value == 0);
        V = false;
    }

    private void SetConditionLoad16(int value)
    {
        N = BitTest(value, 15);
        Z = (value == 0);
        V = false;
    }

    /*
**  BitTest - test bit by bit position (0-7)
     */
    private boolean BitTest(int arg, int bitnum)
    {
        if ((arg & (1 << bitnum)) > 0)
        {
            return (true);
        } else
        {
            return (false);
        }
    }

    private void StoreReg(Register reg, int value)
    {
        switch (reg)
        {
            case A:
                ACCA = value;
                break;
            case B:
                ACCB = value;
                break;
            case CC:
                SetConditionCode(value);
                break;
            case DP:
                DP = value;
                break;  
            case D:
                ACCA = (value >> 8);
                ACCB = (value & 0xFF);
                break;
            case X:
                X = value;
                break;
            case Y:
                Y = value;
                break;
            case PC:
                PC = value;
                break;
            case UP:
                UP = value;
                break;
            case SP:
                SP = value;
                NMIInhibit = false;
                break;
        }
    }

/*
**      StoreValue - store value in memory.
**      pass -1 for location to use last known (e.g.: ASL,ASR)
*/
    private void StoreValue(int location, int value)
    {
        if(location < 0)
            mem.MemWrite(lastLocation, value);
        else
            mem.MemWrite(location, value);
    }
        
    private int GetReg(Register reg)
    {
        int retval;
        switch (reg)
        {
            case A:
                retval = ACCA;
                break;
            case B:
                retval = ACCB;
                break;
            case X:
                retval = X;
                break;
            case Y:
                retval = Y;
                break;
            case CC:
                retval = GetConditionCode();
                break;
            case D:
                retval = ((ACCA << 8) + (ACCB));
                break;
            case SP:
                retval = SP;
                break;
            case UP:
                retval = UP;
                break;
            case DP:
                retval = DP;
                break;
            case PC:
                retval = PC;
                break;
            default:
                retval = 0;
                break;
        }
        return (retval);
    }

    private int GetArgument(Instruction.AddressMode mode)
    {
        int value;
        switch (mode)
        {
            case IMMEDIATE:
                value = ImmediateValue(CurrentInstruction.commandlength - 1);
                break;
            case DIRECT:
                value = DirectValue();
                break;
            case EXTENDED:
                value = ExtendedValueByAddress();
                break;
            case INDEXED:
                value = IndexedValue(false, false);
                break;
            default:
                value = 0;
                break;
        }
        return (value);
    }

    private int GetArgument16(Instruction.AddressMode mode)
    {
        int value = 0;
        switch (mode)
        {
            case IMMEDIATE:
                value = ImmediateValue(2);
                break;
            case DIRECT:
                value = DirectValue16();
                break;
            case EXTENDED:
                value = ExtendedValue16ByAddress();
                break;
            case INDEXED:
                value = IndexedValue(true, false);
                break;
        }
        return (value);
    }

    private int ImmediateValue(int length)
    {
        int result;
        // PC has incremented past the ID
        if (length > 1)
        {
            result = (mem.MemRead(PC) << 8) + mem.MemRead(PC + 1);
        } else
        {
            result = mem.MemRead(PC);
        }
        PC += length;
        return (result);
    }

    private int DirectValue()
    {
        int result = mem.MemRead((DP << 8) + mem.MemRead(PC));
        lastLocation = (DP << 8) + mem.MemRead(PC);
        PC++;
        return (result);
    }

    private int DirectValue16()
    {
        int result = (mem.MemRead((DP << 8) + mem.MemRead(PC)) << 8) + mem.MemRead((DP << 8) + mem.MemRead(PC) + 1);
        lastLocation = (DP << 8) + mem.MemRead(PC);
        PC++;
        return (result);
    }
    
    private int DirectAddress ()
    {
        int result = ((DP << 8) + mem.MemRead(PC++));
        return (result);
    }

    private int ExtendedAddress()
    {
        int result;
        result = (mem.MemRead(PC) << 8) + (mem.MemRead(PC+1));
        lastLocation = result;
        PC+=2;
        return (result);
    }
    
    private int ExtendedValueByAddress()
    {
        int address, result;
        address = (mem.MemRead(PC) << 8) + (mem.MemRead(PC + 1));
        result = mem.MemRead(address);
        lastLocation = address;
        PC += 2;
        return (result);
    }

    private int ExtendedValue16ByAddress()
    {
        int address, result;
        address = (mem.MemRead(PC << 8) + (mem.MemRead(PC + 1)));
        result = (mem.MemRead(address) + mem.MemRead(address + 1));
        lastLocation = address;
        return result;
    }

    /*
**  IndexedValue - Get 8-bit or 16-bit value for instructions using indexed addressing
     */
    private int IndexedValue(boolean bSize, boolean bAddress)
    {
        int result = 0, postbyte, postbyte2, index;
        int rVal, offsetval;
        Register reg;
        postbyte = mem.MemRead(PC++);
        rVal = (postbyte >> 5) & 0x3;
        switch (rVal)
        {
            case 0:
                reg = Register.X;
                index = X;
                break;
            case 1:
                reg = Register.Y;
                index = Y;
                break;
            case 2:
                reg = Register.UP;
                index = UP;
                break;
            case 3:
                reg = Register.SP;
                index = SP;
                break;
            default:
                reg = Register.A;
                index = 0;
                break;
        }
        offsetval = (postbyte & 0x1F);

        if (!BitTest(postbyte, 7)) //signed offset 5 bit, direct
        {
            moreclocks = 1;
            index = index + TwosComplement5Bit(offsetval);
            if (bSize)
            {
                result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
            } else
            {
                result = mem.MemRead(index);
            }
        } else // Bit 7 == 1
        {
            switch (offsetval)
            {
                case 0x4: //Constant offset, direct, no offset
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x14: //Constant offset, indirect, no offset
                    moreclocks = 3;
                    index = ((mem.MemRead(index) << 8) + (mem.MemRead(index + 1)));
                    if (bSize)
                    {
                        result = mem.MemRead(index) << 8 + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x8: // Constant offset, 8 bit, direct
                    moreclocks = 1;
                    postbyte2 = mem.MemRead(PC++);
                    index = index + TwosComplement8Bit(postbyte2);
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x18: // Constant offset, 8 bit, indirect
                    moreclocks  = 4;
                    postbyte2 = mem.MemRead(PC++);
                    index = index + TwosComplement8Bit(postbyte2);
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x9: // Constant offset, 16 bit, direct
                    moreclocks = 4;
                    postbyte2 = mem.MemRead(PC++);
                    postbyte2 = ((postbyte2 << 8) + mem.MemRead(PC++));
                    index = index + TwosComplement16Bit(postbyte2);
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x19: // Constant offset, 16 bit, indirect
                    moreclocks = 7;
                    postbyte2 = mem.MemRead(PC++);
                    postbyte2 = ((postbyte2 << 8) + mem.MemRead(PC++));
                    index = index + TwosComplement16Bit(postbyte2);
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x6: // Accumulator offset A, direct
                    moreclocks = 1;
                    index = index + TwosComplement8Bit(GetReg(Register.A));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x16: // Accumulator offset A, indirect
                    moreclocks = 4;
                    index = index + TwosComplement8Bit(GetReg(Register.A));
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x5: // Accumulator offset B, direct
                    moreclocks = 1;
                    index = index + TwosComplement8Bit(GetReg(Register.B));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x15: // Accumulator offset B, indirect
                    moreclocks = 4;
                    index = index + TwosComplement8Bit(GetReg(Register.B));
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0xB:  // Accumulator offset D, direct
                    moreclocks = 4;
                    index = index + TwosComplement16Bit(GetReg(Register.D));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x1B: // Accumulator offset D, indirect
                    moreclocks = 7;
                    index = index + TwosComplement16Bit(GetReg(Register.D));
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x0: // Auto post-increment by 1
                    moreclocks = 2;
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    IncReg(reg);
                    break;
                case 0x1: // Auto post-increment by 2
                    moreclocks = 3;
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    IncReg(reg);
                    IncReg(reg);
                    break;
                case 0x11: // Auto post-increment by 2, indirect
                    moreclocks = 6;
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if (bSize)
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    else
                        result = mem.MemRead(index);
                    IncReg(reg);
                    IncReg(reg);
                    break;
                case 0x2: // Auto pre-decrement by 1
                    moreclocks = 2;
                    DecReg(reg);
                    index = GetReg(reg);
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x3: // Auto pre-decrement by 2
                    moreclocks = 3;
                    DecReg(reg);
                    DecReg(reg);
                    index = GetReg(reg);
                    if (bSize)
                    {
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index + 1);
                    } else
                    {
                        result = mem.MemRead(index);
                    }
                    break;
                case 0x13: // Auto pre-decrement by 2, indirect
                    moreclocks = 6;
                    DecReg(reg);
                    DecReg(reg);
                    index = GetReg(reg);
                    index = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    if(bSize)
                        result = ((mem.MemRead(index) << 8) + mem.MemRead(index + 1));
                    else
                        result = mem.MemRead(index);
                    break;
                case 0xC: // 8 bit offset from PC, direct
                    moreclocks = 1;
                    index = (PC + 1) + TwosComplement8Bit(mem.MemRead(PC));
                    if(bSize)
                        result = (mem.MemRead(index) << 8) + mem.MemRead(index+1);
                    else
                        result = (mem.MemRead(index));
                    PC++;
                    break;
                case 0x1C: // 8 bit offset from PC, indirect
                    moreclocks = 4;
                    index = (PC + 1) + TwosComplement8Bit(mem.MemRead(PC));
                    index = (mem.MemRead(index) + mem.MemRead(index+1));
                    if(bSize)
                        result =  (mem.MemRead(index) + mem.MemRead(index+1));
                    else
                        result = (mem.MemRead(index));
                    PC++;
                    break;
                case 0xD: // 16 bit offset from PC, direct
                    moreclocks = 5;
                    index = (PC + 2) + TwosComplement16Bit((mem.MemRead(PC) << 8) + (mem.MemRead(PC + 1)));
                    if(bSize)
                        result = (mem.MemRead(index) + mem.MemRead(index+1));
                    else
                        result = (mem.MemRead(index));
                    PC+=2;
                    break;
                case 0x1D: // 16 bit offset from PC, indirect
                    moreclocks = 8;
                    index = (PC + 2) + TwosComplement16Bit((mem.MemRead(PC) << 8) + (mem.MemRead(PC + 1)));
                    index = (mem.MemRead(index) + (mem.MemRead(index + 1)));
                    if(bSize)
                        result = (mem.MemRead(index) + mem.MemRead(index+1));
                    else
                        result = (mem.MemRead(index));
                    PC+=2;
                    break;
                case 0x1F: // extended indirect
                    moreclocks = 5;
                    index = ((mem.MemRead(PC) << 8) + mem.MemRead(PC + 1));
                    index = ((mem.MemRead(index) << 8) + (mem.MemRead(index + 1)));
                    if(bSize)
                        result = (mem.MemRead(index) + mem.MemRead(index + 1));
                    else
                        result = (mem.MemRead(index));
                    PC+=2;
                    break;
            }

        }
        lastLocation = index;
        return ((bAddress ? index : result));
    }

    private int TwosComplement5Bit(int arg)
    {
        int Value = arg;
        if (BitTest(arg, 4))
        {
            Value = -32 + arg;
        }
        return Value;
    }

    private int TwosComplement8Bit(int arg)
    {
        int Value = arg;
        if (BitTest(arg, 7))
        {
            Value = -256 + arg;
        }
        return Value;
    }

    private int TwosComplement16Bit(int arg)
    {
        int Value = arg;
        if (BitTest(arg, 15))
        {
            Value = -65536 + arg;
        }
        return Value;
    }

    private void IncReg(Register reg)
    {
        int iValue = GetReg(reg);
        if (iValue < ((reg == Register.A) || (reg == Register.B) ? 255 : 65535))
        {
            iValue++;
        }
        else
        {
            iValue = 0;
        }
        StoreReg(reg, iValue);
    }

    private void DecReg(Register reg)
    {
        int iValue = GetReg(reg);
        if (iValue != 0)
        {
            iValue--;
        }
        else
        {
            iValue = ((reg == Register.A) || (reg == Register.B) ? 255 : 65535);
        }
        StoreReg(reg, iValue);
    }

    public int GetClockDelay()
    {
        return (MAXCLOCKDELAY - ClockDelay);
    }

    public int GetRealClockDelay()
    {
        return (ClockDelay);
    }

/*
**      GetConditionCode - return condition code value based on condition bits.
**      Used when pushing condition code onto stack.
*/
    private int GetConditionCode()
    {
        int result = 0x0;
        result += (C ? 1 : 0);
        result += (V ? 2 : 0);
        result += (Z ? 4 : 0);
        result += (N ? 8 : 0);
        result += (I ? 0x10 : 0);
        result += (H ? 0x20 : 0);
        result += (F ? 0x40 : 0);
        result += (E ? 0x80 : 0);
        return (result);
    }
    
    private int GetJump (Instruction.AddressMode mode)
    {
        int result = 0;
            
        switch (mode)
        {
            case DIRECT:
                result = DirectAddress();
                break;
            case EXTENDED:
                result = ExtendedAddress();
                break;
            case INDEXED:
                result = IndexedValue(true, true);
                break;
        }
        return (result);
    }

    /*
**      SetConditionCode - Set condition bits based on CC value.  Used when
**      pulling from stack.
     */
    private void SetConditionCode(int ivalue)
    {
        C = BitTest(ivalue, 0);
        V = BitTest(ivalue, 1);
        Z = BitTest(ivalue, 2);
        N = BitTest(ivalue, 3);
        I = BitTest(ivalue, 4);
        H = BitTest(ivalue, 5);
        F = BitTest(ivalue, 6);
        E = BitTest(ivalue, 7);
    }
    /*
**      branch - do branch for all Bxx instructions.  Calculate positive or negative
        offset of PC using 8 bit two's complement value from the instruction
    */
    private void branch ()
    {
        int rel = mem.MemRead(PC);
        rel = TwosComplement8Bit(rel);
        PC = PC + 1 + rel;
    }
    
    private void longbranch ()
    {
        int rel = ((mem.MemRead(PC) << 8) + (mem.MemRead(PC+1)));
        rel = TwosComplement16Bit(rel);
        PC = PC + 2 + rel;
    }
    
/*
**      push8 - Push 8 bit value onto stack
**      6809 pre-decrements, 6800 post-decrements
*/
    private void push8 (Register reg, int arg)
    {
        if(reg == Register.SP)
        {
            SP--;
            mem.MemWrite(SP, arg);
        }
        else
        {
            UP--;
            mem.MemWrite(UP, arg);
        }     
        
    }

/*
**      pull8 - Pull 8 bit value from stack
*/
    private int pull8 (Register reg)
    {
        if(reg == Register.SP)
        {
            return (mem.MemRead(SP++));
        }
        else
        {
            return (mem.MemRead(UP++));
        } 
    }

/*
**      push16 - Push 16 bit value onto stack
**      6809 pre-decrements, 6800 post-decrements
*/
    private void push16 (Register reg, int arg)
    {
        if(reg == Register.SP)
        {
            SP--;
            mem.MemWrite(SP, arg & 0xFF);
            SP--;
            mem.MemWrite(SP, (arg >> 8));
            
        }
        else
        {
            UP--;
            mem.MemWrite(UP, arg & 0xFF);
            UP--;
            mem.MemWrite(UP, (arg >> 8));
            
        }
        
    }
/*
**      pull16 - Pull 16 bit value from stack
*/
    private int pull16 (Register reg)
    {
        int result;
        if(reg == Register.SP)
        {
            result = mem.MemRead(SP);
            SP++;
            result = (result << 8) + mem.MemRead(SP);
            SP++;
        
        }
        else
        {
            result = mem.MemRead(UP);
            UP++;
            result = (result << 8) + mem.MemRead(UP);
            UP++;
        }
        return (result);
    }
    
    public void NMI ()
    {
        if(!CWAIFlag)
        {
            E = true;
            push16(Register.SP, PC);
            push16(Register.SP, UP);
            push16(Register.SP, Y);
            push16(Register.SP, X);
            push8(Register.SP, DP);
            push8(Register.SP, ACCB);
            push8(Register.SP, ACCA);
            push8(Register.SP, GetConditionCode());
            I = true;
            F = true;
            PC = (mem.MemRead(NMIVECTOR) << 8) + mem.MemRead(NMIVECTOR+1);
        }
        else
        {
            CWAIFlag = false;
            PC = (mem.MemRead(NMIVECTOR) << 8) + mem.MemRead(NMIVECTOR+1);
        }
        NMIFlag = false;
        SyncFlag = false;
        state = CommandStates.COMMAND;
    }
    
    public void IRQ ()
    {
        if(!I)
        {
            E = true;
            push16(Register.SP, PC);
            push16(Register.SP, UP);
            push16(Register.SP, Y);
            push16(Register.SP, X);
            push8(Register.SP, DP);
            push8(Register.SP, ACCB);
            push8(Register.SP, ACCA);
            push8(Register.SP, GetConditionCode());
            I = true;
            F = true;
            PC = (mem.MemRead(IRQVECTOR) << 8) + mem.MemRead(IRQVECTOR+1);
            state = CommandStates.COMMAND;
        }
        IRQFlag = false;
        SyncFlag = false;
    }
    
    public void FIRQ ()
    {
        if(!F)
        {
            E = false;
            push16(Register.SP, PC);
            push8(Register.SP, GetConditionCode());
            F = true;
            I = true;
            PC = (mem.MemRead(FIRQVECTOR) << 8) + mem.MemRead(FIRQVECTOR+1);
            state = CommandStates.COMMAND;
        }
        FIRQFlag = false;
        SyncFlag = false;
    }
    
    public void SetClockDelay(int iValue)
    {
        if(iValue < MINCLOCKDELAY)
            iValue = MINCLOCKDELAY;
        if(iValue > MAXCLOCKDELAY)
            iValue = MAXCLOCKDELAY;
        ClockDelay = MAXCLOCKDELAY - iValue;
        if(ClockDelay <= 0)
            ClockDelay = MINCLOCKDELAY;
    }
}
