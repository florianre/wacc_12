package arm11;

import java.util.Stack;

public class AccumulatorMachine {

  private static final Register ACCUMULATOR = ARM11Registers.R10;
  private static final Register RESERVED = ARM11Registers.R11;
  private static final boolean DEBUGGING = false;
  private int borrowedRegCount;
  private Stack<Register> freeRegisters;

  public AccumulatorMachine() {

    this.freeRegisters = new Stack<>();
    this.borrowedRegCount = 0;
  }

  private boolean inAccumulatorMode() {

    return borrowedRegCount > 0;
  }

  public void resetFreeRegisters() {

    freeRegisters.clear();
    freeRegisters.push(ARM11Registers.R12);
    freeRegisters.push(ARM11Registers.R11);
    freeRegisters.push(ARM11Registers.R10);
    freeRegisters.push(ARM11Registers.R9);
    freeRegisters.push(ARM11Registers.R8);
    freeRegisters.push(ARM11Registers.R7);
    freeRegisters.push(ARM11Registers.R6);
    freeRegisters.push(ARM11Registers.R5);
    freeRegisters.push(ARM11Registers.R4);
  }

  public Register peekFreeRegister() {

    Register register = freeRegisters.peek();
    return (register == RESERVED ? ACCUMULATOR : register);
  }

  public Register popFreeRegister() {

    Register register = freeRegisters.peek();
    if (register == RESERVED) {
      register = ACCUMULATOR;
      borrowedRegCount++;
    } else {
      register = freeRegisters.pop();
    }
    if (DEBUGGING) {
      System.err.print("pop: ");
      freeRegisters.forEach(System.err::print);
      System.err.println();
    }
    return register;
  }

  public void pushFreeRegister(Register register) {

    if (borrowedRegCount > 0) {
      borrowedRegCount--;
    } else {
      if (freeRegisters.contains(register)) {
        throw new RuntimeException();
      }
      freeRegisters.push(register);
    }
    if (DEBUGGING) {
      System.err.print("push: ");
      freeRegisters.forEach(System.err::print);
      System.err.println();
    }
  }

  public InstructionList getInstructionList(InstructionType inst, Register dst,
                                            Operand op) {

    InstructionList result = new InstructionList();
    if (inst.isLoad()) {
      return loadInstructions(inst, dst, op);
    } else if (inst.isMove()) {
      return moveInstructions(inst, dst, op);
    } else if (inst.isLogical()) {
      return logicalInstructions(inst, dst, op);
    } else if (inst.isArithmetic()) {
      if (op instanceof Register) {
        return arithmeticInstructions(inst, dst, (Register) op);
      }
    } else if (inst.isStore()) {
      if (op instanceof Register) {
        return storeInstructions(inst, dst, (Register) op, new Immediate(0L));
      }
    }
    return result;
  }

  public InstructionList getInstructionList(InstructionType inst, Register dst,
                                            Register src, Operand op) {

    InstructionList result = new InstructionList();
    if (op instanceof Register) {
      Register src2 = (Register) op;
      if (inst.isArithmetic()) {
        return arithmeticInstructions(inst, dst, src, src2);
      } else if (inst.isLogical()) {
        return logicalInstructions(inst, dst, src, src2);
      }
    } else if (op instanceof Immediate) {
      Immediate imm = (Immediate) op;
      if (inst.isLoad()) {
        return loadInstructions(inst, dst, src, imm);
      } else if (inst.isStore()) {
        return storeInstructions(inst, dst, src, imm);
      }
    } else if (op instanceof Shift) {
      if (inst.isLogical()) {
        return logicalInstructions(inst, dst, src, (Shift) op);
      }
    }
    return result;
  }

  /***********************
   * Arithmetic Instructions
   ***********************/

  private InstructionList arithmeticInstructions(InstructionType inst,
                                                 Register dst, Register src1,
                                                 Register src2) {

    InstructionList result = new InstructionList();

    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPop(RESERVED));
      src1 = RESERVED;
    }
    switch (inst) {
      case ADD:
        result.add(InstructionFactory.createAdd(dst, src1, src2));
        break;
      case ADDS:
        // Used in visitAddOper
        result.add(InstructionFactory.createAdds(dst, src1, src2));
        break;
      case SUB:
        result.add(InstructionFactory.createSub(dst, src1, src2));
        break;
      case SUBS:
        result.add(InstructionFactory.createSubs(dst, src1, src2));
        break;
      default:
        break;
    }
    return result;
  }

  private InstructionList arithmeticInstructions(InstructionType inst,
                                                 Register src1, Register src2) {
    InstructionList result = new InstructionList();
    Register dst1 = src1;
    Register dst2 = src2;
    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPop(RESERVED));
      src1 = RESERVED;
      dst2 = RESERVED;
    }
    switch (inst) {
      case SMULL:
        result.add(InstructionFactory.createSmull(dst1, dst2, src1, src2));
        break;
      case DIVMOD:
        result.add(InstructionFactory.createMove(ARM11Registers.R0, src1))
              .add(InstructionFactory.createMove(ARM11Registers.R1, src2));
        break;
      default:
        break;
    }
    return result;
  }

  /**************************
   * Load Instructions
   **************************/

  private InstructionList loadInstructions(InstructionType inst, Register dst,
                                           Operand op) {
    InstructionList result = new InstructionList();
    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPush(dst));
    }
    switch (inst) {
      case LDR:
        result.add(InstructionFactory.createLoad(dst, op));
        break;
    }
    return result;
  }

  private InstructionList loadInstructions(InstructionType inst, Register dst,
                                           Register src, Immediate offset) {
    InstructionList result = new InstructionList();
    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPush(dst));
      // TODO: Remove magic number - once merged
      offset = new Immediate(offset.getValue() + (borrowedRegCount * 4));
    }
    switch (inst) {
      case LDR:
        result.add(InstructionFactory.createLoad(dst, src, offset));
        break;
      case LDRSB:
        result.add(InstructionFactory.createLoadStoredByte(dst, src, offset));
      default:
        break;
    }
    return result;
  }

  /*************************
   * Logical Instructions
   ************************/

  private InstructionList logicalInstructions(InstructionType inst,
                                              Register dst, Register src1,
                                              Register src2) {

    InstructionList result = new InstructionList();

    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPop(RESERVED));
      src1 = RESERVED;
    }
    switch (inst) {
      case AND:
        result.add(InstructionFactory.createAnd(dst, src1, src2));
        break;
      case ORR:
        result.add(InstructionFactory.createOrr(dst, src1, src2));
        break;
      default:
        break;
    }
    return result;
  }

  private InstructionList logicalInstructions(InstructionType inst,
                                             Register src1, Register src2,
                                             Shift shift) {
    InstructionList result = new InstructionList();

    if (inAccumulatorMode()) {
      src1 = RESERVED;
    }
    switch (inst) {
      case CMP:
        result.add(InstructionFactory.createCompare(src1, src2, shift));
        break;
      default:
        break;
    }
    return result;
  }

  private InstructionList logicalInstructions(InstructionType inst,
                                              Register src, Operand op) {

    InstructionList result = new InstructionList();

    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPop(RESERVED));
      src = RESERVED;
    }
    switch (inst) {
      case CMP:
        result.add(InstructionFactory.createCompare(src, op));
      default:
        break;
    }
    return result;
  }

  /**************************
   * Move Instructions
   **************************/

  private InstructionList moveInstructions(InstructionType inst, Register dst,
                                           Operand op) {

    InstructionList result = new InstructionList();
    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPush(dst));
    }
    switch (inst) {
      case MOV:
        result.add(InstructionFactory.createMove(dst, op));
        break;
    }
    return result;
  }

  /**************************
   * Store Instructions
   *************************/

  private InstructionList storeInstructions(InstructionType inst, Register dst,
                                            Register src, Immediate offset) {

    InstructionList result = new InstructionList();
    if (inAccumulatorMode()) {
      result.add(InstructionFactory.createPush(dst));
      // TODO: Remove magic number - once merged
      offset = new Immediate(offset.getValue() + (borrowedRegCount * 4));
    }
    switch (inst) {
      case STR:
        result.add(InstructionFactory.createStore(dst, src, offset));
        break;
      case STRB:
        result.add(InstructionFactory.createStoreByte(dst, src, offset));
      default:
        break;
    }
    return result;
  }
}
