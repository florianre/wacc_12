package arm11;

import static arm11.ARM11Registers.SP;
import static arm11.InstructionType.*;

public class InstructionFactory {

  public static final String AEABI_IDIV = "__aeabi_idiv";
  private static final String AEABI_IDIVMOD = AEABI_IDIV + "mod";

  private static String getOptionalHash(Operand op) {
    return op.isImmediate() ? "#" : "";
  }

  public static Instruction createLoad(Register dst,  Operand op) {
    return () -> LDR + " " + dst + ", " + (op instanceof Immediate || op
                 instanceof Label ? "=" : "") + op;
  }

  public static Instruction createBranch(Label label) {
    return () -> B + " " + label;
  }

  public static Instruction createBranchLink(Label label) {
    return () -> BL + " " + label;
  }

  public static Instruction createBranchEqual(Label label) {
    return () -> BEQ + " " + label;
  }

  public static Instruction createLabel(Label label) {
    return () -> label + ":";
  }

  public static Instruction createPush(Register register) {
    return () -> PUSH + " {" + register + "}";
  }

  public static Instruction createPop(Register register) {
    return () -> POP + " {" + register + "}";
  }

  public static Instruction createLTORG() {
    return LTORG::toString;
  }

  public static Instruction createText() {
    return TEXT::toString;
  }

  public static Instruction createGlobal(Label mainLabel) {
    return () -> GLOBAL + " " + mainLabel;
  }

  public static Instruction createSub(Register dst, Register rn, Operand imm) {
    return () -> SUB + " " + dst + ", " + rn + ", #" + imm;
  }

  public static Instruction createMove(Register dst, Operand op) {
    return () -> MOV + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createAdd(Register dst, Register src, Operand op) {
    return () -> ADD + " " + dst + ", " + src + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createCompare(Register reg, Operand op) {
    return () -> CMP + " " + reg + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createCompare(Register reg, Operand op1,
                                          Operand op2) {
    return () -> CMP + " " + reg + ", " + getOptionalHash(op1) + op1 + ", "
                 + op2;
  }

  public static Instruction createLoadNotEqual(Register register,
                                                Label label) {
    return () -> LDRNE + " " + register + ", =" + label;
  }

  public static Instruction createLoadEqual(Register register,
                                             Label label) {
    return () -> LDREQ + " " + register + ", =" + label;
  }

  public static Instruction createWord(int length) {
    return () -> WORD + " " + new Word(length);
  }

  public static Instruction createAscii(String ascii) {
    return () -> ASCII + " " + new Ascii(ascii);
  }

  public static Instruction createStore(Register src, Register base,
                                        Operand offset) {
    return () -> {
      int offsetValue = Integer.parseInt(offset.toString());
      String forceIncrement = offsetValue < 0 ? "!" : "";
      String addOffset = offsetValue != 0 ? ", #" + offset : "";
      return STR + " " + src + ", [" + base
          + addOffset + "]" + forceIncrement;
    };
  }

  public static Instruction createStoreByte(Register src, Register base,
                                             Operand offset) {
    return () -> {
      int offsetValue = Integer.parseInt(offset.toString());
      String forceIncrement = offsetValue < 0 ? "!" : "";
      String addOffset = offsetValue != 0 ? ", #" + offset : "";
      return STRB + " " + src + ", [" + base
          + addOffset + "]" + forceIncrement;
    };
  }

  public static Instruction createData() {
    return DATA::toString;
  }

  public static Instruction createLoad(Register dst, Register base,
                                        Immediate offset) {
    return () -> {
      String addOffset = !offset.toString().equals("0") ? ", #" + offset : "";
      return LDR + " " + dst + ", [" + base + addOffset + "]";
    };
  }

  // TODO: check this out
  public static Instruction createLoadStoredByte(Register dst, Register base,
                                                  Immediate offset) {
    return () -> {
      String addOffset = !offset.toString().equals("0") ? ", #" + offset : "";
      return LDRSB + " " + dst + ", [" + base + addOffset + "]";
    };
  }

  public static Instruction createEOR(Register dst, Register src,
                                       Operand imm) {
    return () -> EOR + " " + dst + ", " + src + ", #" + imm;
  }

  public static Instruction createRSBS(Register dst,
                                        Register src,
                                        Operand imm) {
    return () -> RSBS + " " + dst + ", " + src + ", #" + imm;
  }

  public static Instruction createAdds(Register dst, Register src,
                                        Operand op) {
    return () -> ADDS + " " + dst + ", " + src + ", "
        + getOptionalHash(op) + op;
  }

  public static Instruction createMovEq(Register dst,  Operand op) {
    return () -> MOVEQ + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createMovNe(Register dst,  Operand op) {
    return () -> MOVNE + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createMovGt(Register dst,  Operand op) {
    return () -> MOVGT + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createMovLe(Register dst,  Operand op) {
    return () -> MOVLE + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createMovGe(Register dst,  Operand op) {
    return () -> MOVGE + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createMovLt(Register dst,  Operand op) {
    return () -> MOVLT + " " + dst + ", " + getOptionalHash(op) + op;
  }

  public static Instruction createAnd(Register dst,  Register src1,
                                       Register src2) {
    return () -> AND + " " + dst + ", " + src1 + ", " + src2;
  }

  public static Instruction createOrr(Register dst, Register src1,
                                       Register src2) {
    return () -> ORR + " " + dst + ", " + src1 + ", " + src2;
  }

  public static Instruction createSubs(Register dst,  Register src,
                                        Operand op) {
    return () -> SUBS + " " + dst + ", " + src + ", "
        + getOptionalHash(op) + op;
  }

  public static Instruction createSmull(Register dst1, Register dst2,
                                         Register src1, Register src2) {
    return () -> SMULL + " " + dst1 + ", " + dst2 + ", " + src1 + ", " + src2;
  }

  public static Instruction createDiv() {
    return () -> BL + " " + AEABI_IDIV;
  }

  public static Instruction createMod() {
    return () -> BL + " " + AEABI_IDIVMOD;
  }

  public static Instruction createBranchLinkEqual(Label label) {

    return () -> BLEQ + " " + label;
  }

  public static Instruction createBranchLinkVS(Label label) {

    return () -> BLVS + " " + label;
  }

  public static Instruction createBranchLinkNotEqual(Label label) {

    return () -> BLNE + " " + label;
  }

  public static Instruction createAdd(Register dst,
                                      Register src1,
                                      Register src2, Operand op) {
    return () -> ADD + " " + dst + ", " + src1 + ", " + src2 + ", " + op;
  }

  public static Instruction createLoadLessThan(Register dst, Label label) {

    return () -> LDRLT + " " + dst + ", =" + label;
  }

  public static Instruction createLoadCS(Register dst, Label label) {

    return () -> LDRCS + " " + dst + ", =" + label;
  }

  public static Instruction createBranchLinkLT(Label label) {

    return () -> BLLT + " " + label;
  }

  public static Instruction createBranchLinkCS(Label label) {

    return () -> BLCS + " " + label;
  }

  public static Instruction getAddInstruction(boolean isStoredByte,
                                              Register result,
                                              Register helper) {
    if (isStoredByte) {
      return InstructionFactory.createAdd(result, result, helper);
    } else {
      Shift shift = new Shift(Shift.Shifts.LSL, 2);
      return InstructionFactory.createAdd(result, result, helper, shift);
    }
  }

  public static Instruction mutateStackPointer(InstructionType type,
                                               Immediate imm) {
    switch (type) {
      case ADD:
        return InstructionFactory.createAdd(SP, SP, imm);
      case SUB:
      default:
        return InstructionFactory.createSub(SP, SP, imm);
    }
  }

  public static Instruction getLoadInstructionForElem(boolean isStoredByte,
                                                      Register dst,
                                                      Register src,
                                                      long offset) {
    if (isStoredByte) {
      return InstructionFactory.createLoadStoredByte(dst, src,
          new Immediate(offset));
    } else {
      return InstructionFactory.createLoad(dst, src, new Immediate(offset));
    }
  }

}
