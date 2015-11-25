package wacc;

import antlr.WACCParser;
import arm11.*;
import bindings.Binding;
import bindings.Type;
import bindings.Variable;
import org.antlr.v4.runtime.misc.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

// TODO: Do we need top for labels?

public class CodeGenerator extends WACCVisitor<InstructionList> {

  private DataInstructions data;
  private HashSet<InstructionList> helperFunctions;

  private Stack<Register> freeRegisters;

  public CodeGenerator(SymbolTable top) {

    super(top);
    this.data = new DataInstructions();
    this.helperFunctions = new HashSet<>();
    this.freeRegisters = new Stack<>();
  }

  //@Override
//  public InstructionList visitInteger(@NotNull WACCParser.IntegerContext ctx) {
//    InstructionList list = defaultResult();
//    long intValue = Long.valueOf(ctx.INTEGER().getText());
//    list.add(InstructionFactory.createMov());
//    return list;
//  }
  
  @Override
  protected InstructionList defaultResult() {
    return new InstructionList();
  }

  @Override
  protected InstructionList aggregateResult(InstructionList aggregate,
                                            InstructionList nextResult) {
    aggregate.add(nextResult);
    return aggregate;
  }

  @Override
  public InstructionList visitProg(WACCParser.ProgContext ctx) {
    resetFreeRegisters();
    String scopeName = Scope.PROG.toString();
    changeWorkingSymbolTableTo(scopeName);
    InstructionList program = defaultResult();
    InstructionList main = visitMain(ctx.main());
    program.add(data.getInstructionList());
    program.add(InstructionFactory.createText());
    Label mainLabel = new Label(WACCVisitor.Scope.MAIN.toString());
    program.add(InstructionFactory.createGlobal(mainLabel));
    program.add(main);
    goUpWorkingSymbolTable();
    return program;
  }

  private void resetFreeRegisters() {
    freeRegisters.clear();
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R4));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R5));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R6));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R7));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R8));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R9));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R10));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R11));
    freeRegisters.push(ARM11Registers.getRegister(ARM11Registers.Reg.R12));
  }

  @Override
  public InstructionList visitMain(WACCParser.MainContext ctx) {
    String scopeName = Scope.MAIN.toString();
    changeWorkingSymbolTableTo(scopeName);

    // TODO: Add the data and helperFunctions sections above and below main

    InstructionList list = defaultResult();

    Label label = new Label(Scope.MAIN.toString());
    list.add(InstructionFactory.createLabel(label));

    Register register
        = ARM11Registers.getRegister(ARM11Registers.Reg.LR);
    list.add(InstructionFactory.createPush(register));

    list.add(allocateSpaceOnStack());
    list.add(visitChildren(ctx));
    list.add(deallocateSpaceOnStack());

    Register r0 = ARM11Registers.getRegister(ARM11Registers.Reg.R0);
    Operand value = new Immediate((long) 0);
    list.add(InstructionFactory.createLoad(r0, value));

    register = ARM11Registers.getRegister(ARM11Registers.Reg.PC);
    list.add(InstructionFactory.createPop(register));

    list.add(InstructionFactory.createLTORG());

    goUpWorkingSymbolTable();
    return list;
  }


  private InstructionList allocateSpaceOnStack() {
    InstructionList list = defaultResult();
    List<Binding> variables = workingSymbolTable.filterByClass(Variable.class);
    long stackSpaceSize = 0;
    for (Binding b : variables) {
      Variable v = (Variable) b;
      stackSpaceSize += v.getType().getSize();
    }
    // TODO: deal with '4095', the max size... of something
    if (stackSpaceSize > 0) {
      Operand imm = new Immediate(stackSpaceSize);
      Register sp = ARM11Registers.getRegister(ARM11Registers.Reg.SP);
      list.add(InstructionFactory.createSub(sp, sp, imm));
    }

    for (Binding b : variables) {
      Variable v = (Variable) b;
      stackSpaceSize -= v.getType().getSize();
      long offset = stackSpaceSize;
      v.setOffset(offset);
    }

    return list;
  }

  private InstructionList deallocateSpaceOnStack() {
    InstructionList list = defaultResult();
    List<Binding> variables = workingSymbolTable.filterByClass(Variable.class);
    long stackSpaceSize = 0;
    for (Binding b : variables) {
      Variable v = (Variable) b;
      stackSpaceSize += v.getType().getSize();
    }

    if (stackSpaceSize > 0) {
      Operand imm = new Immediate(stackSpaceSize);
      Register sp = ARM11Registers.getRegister(ARM11Registers.Reg.SP);
      list.add(InstructionFactory.createAdd(sp, sp, imm));
    }

    return list;
  }

  @Override
  public InstructionList visitSkipStat(WACCParser.SkipStatContext ctx) {
    return defaultResult();
  }

  @Override
  public InstructionList visitExitStat(WACCParser.ExitStatContext ctx) {

    InstructionList list = defaultResult();

    Register r0 = ARM11Registers.getRegister(ARM11Registers.Reg.R0);
    Long imm = Long.parseLong(ctx.expr().getText());
    Operand value = new Immediate(imm);
    Label label = new Label("exit");

    list.add(InstructionFactory.createLoad(r0, value));
    list.add(InstructionFactory.createBranchLink(label));

    return list;
  }

  @Override
  public InstructionList visitInitStat(@NotNull WACCParser.InitStatContext ctx) {
    InstructionList list = defaultResult();

    Register reg = freeRegisters.pop();

    Operand op = null;
    Instruction storeInstr = null;
    Register sp  = ARM11Registers.getRegister(ARM11Registers.Reg.SP);
    Variable var = (Variable) workingSymbolTable.get(ctx.ident().getText());
    Operand offset = new Immediate(var.getOffset());


    Type varType = var.getType();
    String text = ctx.assignRHS().getText();
    if (Type.isInt(varType)) {
      long value = Long.parseLong(text);
      op = new Immediate(value);
      storeInstr = InstructionFactory.createStore(reg, sp, offset);
    } else if (Type.isBool(varType)) {
      long value;
      if (text.equals("true")) {
        value = 1;
      } else {
        value = 0;
      }
      op = new Immediate(value);
      storeInstr = InstructionFactory.createStoreBool(reg, sp, offset);
    } else if (Type.isChar(varType)) {
      op = new Immediate(text);
      storeInstr = InstructionFactory.createStore(reg, sp, offset);
    } else if (Type.isString(varType)) {
      op = data.addConstString(text);
      storeInstr = InstructionFactory.createStore(reg, sp, offset);
    }

    list.add(InstructionFactory.createLoad(reg, op));
    list.add(storeInstr);
    freeRegisters.push(reg);
    return list;
  }

  public InstructionList visitPrintStat(
      @NotNull WACCParser.PrintStatContext ctx) {

    InstructionList list = defaultResult();

    // TODO: find the type of what we are printing

    //    if (ctx.expr().returnType != null) {
    //      Type exprType = (Type)ctx.expr().returnType;
    //      System.err.println(exprType);
    //    }
    // We are assuming ints


    if (Type.isString((Type) ctx.expr().returnType)) {
      Register r0 = ARM11Registers.getRegister(ARM11Registers.Reg.R0);
      // TODO: uses freeRegisters (Stack)
      Register r4 = ARM11Registers.getRegister(ARM11Registers.Reg.R4);

      // Assume it saves result in r4
      list.add(visitExpr(ctx.expr()));

      list.add(InstructionFactory.createMov(r0, r4));
      list.add(InstructionFactory.createBranchLink(new Label("p_print_string")));

      InstructionList printHelperFunction = PrintFunctions.printString(data);
      helperFunctions.add(printHelperFunction);
    } else {


    }
    return list;
  }

}
