/*
 * Instruction.java
** Class definition for 6809 instruction
**
** Dave Sherman 03/29/2020
**
** Revisions:
** 
*/

package m6809;

/**
 *
 * @author daves
 */


public class Instruction {
    
    public enum CommandID {
        ABX,
        ADCA,
        ADCB,
        ADDA,
        ADDB,
        ADDD,
        ANDA,
        ANDB,
        ANDCC,
        ASL,
        ASLA,
        ASLB,
        ASR,
        ASRA,
        ASRB,
        BCC,
        LBCC,
        BCS,
        LBCS,
        BEQ,
        LBEQ,
        BGE,
        LBGE,
        BGT,
        LBGT,
        BHI,
        LBHI,
        BHS, // actually BCC
        LBHS, // actually LBCC
        BITA,
        BITB,
        BLE,
        LBLE,
        BLO, // actually BCS
        LBLO, // actually LBCS
        BLS,
        LBLS,
        BLT,
        LBLT,
        BMI,
        LBMI,
        BNE,
        LBNE,
        BPL,
        LBPL,
        BRA,
        LBRA,
        BRN,
        LBRN,
        BSR,
        LBSR,
        BVC,
        LBVC,
        BVS,
        LBVS,
        CLR,
        CLRA,
        CLRB,
        CMPA,
        CMPB,
        CMPD,
        CMPS,
        CMPU,
        CMPX,
        CMPY,
        COM,
        COMA,
        COMB,
        CWAI,
        DAA,
        DEC,
        DECA,
        DECB,
        EORA,
        EORB,
        EXG,
        INC,
        INCA,
        INCB,
        JMP,
        JSR,
        LDA,
        LDB,
        LDD,
        LDS,
        LDU,
        LDX,
        LDY,
        LEAX,
        LEAY,
        LEAS,
        LEAU,
        //LSL,  actually ASL
        //LSLA, actually ASLA
        //LSLB, actually ASLB
        LSR,
        LSRA,
        LSRB,
        MUL,
        NEG,
        NEGA,
        NEGB,
        NOP,
        ORA,
        ORB,
        ORCC,
        PSHS,
        PSHU,
        PULS,
        PULU,
        ROL,
        ROLA,
        ROLB,
        ROR,
        RORA,
        RORB,
        RTI,
        RTS,
        SBCA,
        SBCB,
        SEX,
        STA,
        STB,
        STD,
        STS,
        STU,
        STX,
        STY,
        SUBA,
        SUBB,
        SUBD,
        SWI,
        SWI2,
        SWI3,
        SYNC,
        TFR,
        TST,
        TSTA,
        TSTB,
        INVALID,
    };
    
    public enum AddressMode {
        IMMEDIATE,
        DIRECT,
        EXTENDED,
        INDEXED,
        RELATIVE,
        INHERENT
    };
    
    final CommandID ID;
    final int opcode;
    final AddressMode mode;
    final int cycles;
    final int commandlength;
    
    public Instruction (CommandID ID, int opcode, AddressMode mode, int cycles, int length)
    {
        this.ID = ID;
        this.opcode = opcode;
        this.mode = mode;
        this.cycles = cycles;
        this.commandlength = length;
    }
    
}
