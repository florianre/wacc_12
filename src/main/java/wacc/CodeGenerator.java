package wacc;

import antlr.WACCParser;
import arm11.*;
import bindings.Binding;
import bindings.NewScope;
import bindings.Type;
import bindings.Variable;

import bindings.NewScope;

import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;


import static arm11.HeapFunctions.freePair;


// TODO: make @NotNulls consistent

public class CodeGenerator extends WACCVisitor<InstructionList> {
  
  private AccumulatorMachine accMachine;

  private static final long ADDRESS_SIZE = 4L;
  private static final long PAIR_SIZE = 2 * ADDRESS_SIZE;
  private DataInstructions data;
  private HashSet<InstructionList> helperFunctions;

  public CodeGenerator(SymbolTable<String, Binding> top) {
    super(top);
    this.data = new DataInstructions();
    this.helperFunctions = new HashSet<>();
    this.accMachine = new AccumulatorMachine();
  }

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

  private String getToken(int index){
    String tokenName = WACCParser.tokenNames[index];

    assert(tokenName.charAt(0) != '\'');

    return tokenName.substring(1, tokenName.length() - 1);
  }

  @Override
  public InstructionList visitProg(WACCParser.ProgContext ctx) {
    accMachine.resetFreeRegisters();
    String scopeName = Scope.PROG.toString();
    changeWorkingSymbolTableTo(scopeName);
    InstructionList program = defaultResult();
    // visit all the functions and add their instructions
    InstructionList functions = defaultResult();
    for (WACCParser.FuncContext function : ctx.func()) {
      functions.add(visitFunc(function));
    }
    InstructionList main = visitMain(ctx.main());
    program.add(data.getInstructionList());
    program.add(InstructionFactory.createText());
    Label mainLabel = new Label(WACCVisitor.Scope.MAIN.toString());
    program.add(InstructionFactory.createGlobal(mainLabel));

    program.add(data.getFunctionList());
    program.add(functions).add(main);

    // Add the helper functions
    for (InstructionList instructionList : helperFunctions) {
      program.add(instructionList);
    }

    goUpWorkingSymbolTable();
    return program;
  }

  @Override
  public InstructionList visitMain(WACCParser.MainContext ctx) {
    String scopeName = Scope.MAIN.toString();
    changeWorkingSymbolTableTo(scopeName);
    pushEmptyVariableSet();

    InstructionList list = defaultResult();

    Label label = new Label(Scope.MAIN.toString());
    Register r0 = ARM11Registers.R0;
    Operand imm = new Immediate((long) 0);

    list.add(InstructionFactory.createLabel(label))
        .add(InstructionFactory.createPush(ARM11Registers.LR))
        .add(allocateSpaceOnStack())
        .add(visitChildren(ctx))
        .add(deallocateSpaceOnStack())
        .add(InstructionFactory.createLoad(r0, imm))
        .add(InstructionFactory.createPop(ARM11Registers.PC))
        .add(InstructionFactory.createLTORG());

    goUpWorkingSymbolTable();
    popCurrentScopeVariableSet();
    return list;
  }

  private InstructionList allocateSpaceOnStack() {
    // TODO: account for newScopes within...
    InstructionList list = defaultResult();
    List<Binding> variables = workingSymbolTable.filterByClass(Variable.class);
    long stackSpaceVarSize = 0;
    long stackSpaceParamSize = 0; // Occupied by params

    for (Binding b : variables) {
      Variable v = (Variable) b;
      if (v.isParam()) {
        stackSpaceParamSize += ADDRESS_SIZE;
      } else {
        stackSpaceVarSize += v.getType().getSize();
      }
    }

    // TODO: deal with '4095', the max size... of something
    if (stackSpaceVarSize > 0) {
      Operand imm = new Immediate(stackSpaceVarSize);
      Register sp = ARM11Registers.SP;
      list.add(InstructionFactory.createSub(sp, sp, imm));
    }

    String scopeName = workingSymbolTable.getName();
    Binding scopeB = workingSymbolTable.getEnclosingST().get(scopeName);
    NewScope scope = (NewScope) scopeB;
    scope.setStackSpaceSize(stackSpaceParamSize + stackSpaceVarSize);


    // First pass to add offsets to variables
    long offset = 0;
    for (int i = variables.size() - 1; i >= 0; i--) {
      Variable v = (Variable) variables.get(i);
      if (!v.isParam()) {
        v.setOffset(offset);
        offset += v.getType().getSize();
      }
    }

    offset += 4;

    for (Binding b : variables) {
      Variable v = (Variable)b;
      if (v.isParam()) {
        v.setOffset(offset);
        offset += v.getType().getSize();
      }
    }

    return list;
  }

  private InstructionList deallocateSpaceOnStack() {
    InstructionList list = defaultResult();
    List<Binding> variables = workingSymbolTable.filterByClass(Variable.class);
    // TODO: use NewScope.getStackSpaceSize()
    long stackSpaceSize = 0;
    for (Binding b : variables) {
      Variable v = (Variable) b;
      if (!v.isParam()) {
        stackSpaceSize += v.getType().getSize();
      }
    }

    if (stackSpaceSize > 0) {
      Operand imm = new Immediate(stackSpaceSize);
      Register sp = ARM11Registers.SP;
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

    Register r0 = ARM11Registers.R0;
    Operand value = new Immediate(Long.parseLong(ctx.expr().getText()));
    Label label = new Label("exit");

    list.add(InstructionFactory.createLoad(r0, value));
    list.add(InstructionFactory.createBranchLink(label));

    return list;
  }

  @Override
  public InstructionList visitWhileStat(WACCParser.WhileStatContext ctx) {
    ++whileCount;
    InstructionList list = defaultResult();

    String whileScope = Scope.WHILE.toString() + whileCount;
    changeWorkingSymbolTableTo(whileScope);
    pushEmptyVariableSet();

    Label predicate = new Label("predicate_" + whileCount);
    Label body = new Label("while_body_" + whileCount);
    Operand trueOp = new Immediate((long) 1);

    list.add(InstructionFactory.createBranch(predicate))
        .add(InstructionFactory.createLabel(body))
        .add(allocateSpaceOnStack())
        .add(visitStatList(ctx.statList()))
        .add(deallocateSpaceOnStack())
        .add(InstructionFactory.createLabel(predicate));

    Register result = accMachine.peekFreeRegister();

    popCurrentScopeVariableSet();
    goUpWorkingSymbolTable();

    list.add(visitExpr(ctx.expr()))
        .add(InstructionFactory.createCompare(result, trueOp))
        .add(InstructionFactory.createBranchEqual(body));
    
    accMachine.pushFreeRegister(result);
    
    return list;
  }

  @Override
  public InstructionList visitIfStat(WACCParser.IfStatContext ctx) {

    ++ifCount;
    InstructionList list = defaultResult();

    Register predicate = accMachine.peekFreeRegister();
    list.add(visitExpr(ctx.expr()));
    list.add(InstructionFactory.createCompare(predicate, new Immediate(0L)));
    // predicate no longer required
    accMachine.pushFreeRegister(predicate);
    
    Label elseLabel = new Label("else_" + ifCount);
    list.add(InstructionFactory.createBranchEqual(elseLabel));

    String thenScope = Scope.THEN.toString() + ifCount;
    changeWorkingSymbolTableTo(thenScope);

    pushEmptyVariableSet();
    list.add(allocateSpaceOnStack())
      .add(visitStatList(ctx.thenStat))
      .add(deallocateSpaceOnStack());
    popCurrentScopeVariableSet();

    goUpWorkingSymbolTable();

    Label continueLabel = new Label("fi_" + ifCount);
    list.add(InstructionFactory.createBranch(continueLabel));

    list.add(InstructionFactory.createLabel(elseLabel));

    String elseScope = Scope.ELSE.toString() + ifCount;
    changeWorkingSymbolTableTo(elseScope);

    pushEmptyVariableSet();
    list.add(allocateSpaceOnStack())
      .add(visitStatList(ctx.elseStat))
      .add(deallocateSpaceOnStack());
    popCurrentScopeVariableSet();
    list.add(InstructionFactory.createLabel(continueLabel));

    goUpWorkingSymbolTable();

    return list;
  }

  @Override
  public InstructionList visitInitStat(WACCParser.InitStatContext ctx) {
    String varName = ctx.ident().getText();
    Variable var = (Variable) workingSymbolTable.get(varName);
    long varOffset = var.getOffset();


    InstructionList list = storeToOffset(varOffset,
                                         var.getType(),
                                         ctx.assignRHS());
    addVariableToCurrentScope(varName);
    return list;
  }

  @Override
  public InstructionList visitAssignStat(WACCParser.AssignStatContext ctx) {
    if (ctx.assignLHS().ident() != null){
      String varName = ctx.assignLHS().ident().getText();
      Variable var = getMostRecentBindingForVariable(varName);
      long varOffset = getAccumulativeOffsetForVariable(varName);
      return storeToOffset(varOffset, var.getType(), ctx.assignRHS());
    }
    return visitChildren(ctx);
  }

  private InstructionList storeToOffset(long varOffset,
                                        Type varType,
                                        WACCParser.AssignRHSContext assignRHS) {
    InstructionList list = defaultResult();

    Register reg = accMachine.peekFreeRegister();

    list.add(visitAssignRHS(assignRHS));

    Instruction storeInstr;
    Register sp  = ARM11Registers.SP;
    Operand offset = new Immediate(varOffset);
    if (Type.isBool(varType) || Type.isChar(varType)){
      storeInstr = InstructionFactory.createStoreByte(reg, sp, offset);
    } else {
      storeInstr = InstructionFactory.createStore(reg, sp, offset);
    }

    list.add(storeInstr);
    accMachine.pushFreeRegister(reg);
    return list;
  }

  @Override
  public InstructionList visitReturnStat(WACCParser.ReturnStatContext ctx) {
    InstructionList list = defaultResult();
    Register resultReg = accMachine.peekFreeRegister();
    list.add(visitExpr(ctx.expr()))
        .add(InstructionFactory.createMov(ARM11Registers.R0, resultReg));
    accMachine.pushFreeRegister(resultReg);
    return list;
  }

  @Override
  public InstructionList visitPrintStat(WACCParser.PrintStatContext ctx) {
    InstructionList list = defaultResult();
    Label printLabel;
    InstructionList printFunction = null;
    Type returnType = ctx.expr().returnType;

    if (Type.isString(returnType)) {
      printFunction = PrintFunctions.printString(data);
      printLabel = new Label("p_print_string");
    } else if (Type.isInt(returnType)) {
      printFunction = PrintFunctions.printInt(data);
      printLabel = new Label("p_print_int");
    } else if (Type.isChar(returnType)){
      printLabel = new Label("putchar");
    } else if (Type.isBool(returnType)) {
      printFunction = PrintFunctions.printBool(data);
      printLabel = new Label("p_print_bool");
    } else {
      printFunction = PrintFunctions.printReference(data);
      printLabel = new Label("p_print_reference");
    }

    Register result = accMachine.peekFreeRegister();
    list.add(visitExpr(ctx.expr()))
        .add(InstructionFactory.createMov(ARM11Registers.R0, result))
        .add(InstructionFactory.createBranchLink(printLabel));
    addPrintFunctionToHelpers(printFunction);
    if (ctx.PRINTLN() != null) {
      printNewLine(list);
    }
    accMachine.pushFreeRegister(result);

    return list;
  }

  private void addPrintFunctionToHelpers(InstructionList printFunction) {
    if (printFunction != null) {
      helperFunctions.add(printFunction);
    }
  }

  @Override
  public InstructionList visitLogicalOper(WACCParser.LogicalOperContext ctx) {
    InstructionList list = defaultResult();
    Register dst1 = accMachine.peekFreeRegister();
    list.add(visitComparisonOper(ctx.first));
    if (!ctx.otherExprs.isEmpty()) {
      Register dst2;
      // for loop used instead of visitChildren so only 2 registers used up
      Instruction logicalInstr;
      for (int i = 0; i < ctx.ops.size(); i++) {
        WACCParser.ComparisonOperContext otherExpr = ctx.otherExprs.get(i);
        dst2 = accMachine.peekFreeRegister();

        if (ctx.ops.get(i).getText().equals(getToken(WACCParser.AND))){
          logicalInstr = InstructionFactory.createAnd(dst1, dst1, dst2);
        } else {
          logicalInstr = InstructionFactory.createOrr(dst1, dst1, dst2);
        }

        list.add(visitComparisonOper(otherExpr))
            .add(logicalInstr);
        accMachine.pushFreeRegister(dst2);
      }
    }

    return list;
  }

  @Override
  public InstructionList visitOrderingOper(WACCParser.OrderingOperContext ctx) {
    InstructionList list = defaultResult();

    Register dst1 = accMachine.peekFreeRegister();
    list.add(visitAddOper(ctx.first));
    if (ctx.second != null) {
      Register dst2 = accMachine.peekFreeRegister();
      Operand trueOp = new Immediate((long) 1);
      Operand falseOp = new Immediate((long) 0);
      list.add(visitAddOper(ctx.second))
          .add(InstructionFactory.createCompare(dst1, dst2));
      if (ctx.GT() != null){
        list.add(InstructionFactory.createMovGt(dst1, trueOp))
            .add(InstructionFactory.createMovLe(dst1, falseOp));
      } else if (ctx.GE() != null) {
        list.add(InstructionFactory.createMovGe(dst1, trueOp))
            .add(InstructionFactory.createMovLt(dst1, falseOp));
      } else if (ctx.LT() != null) {
        list.add(InstructionFactory.createMovLt(dst1, trueOp))
            .add(InstructionFactory.createMovGe(dst1, falseOp));
      } else if (ctx.LE() != null) {
        list.add(InstructionFactory.createMovLe(dst1, trueOp))
            .add(InstructionFactory.createMovGt(dst1, falseOp));
      }

      accMachine.pushFreeRegister(dst2);
    }

    return list;
  }

  private void printNewLine(InstructionList list) {
    Label printLabel = new Label("p_print_ln");
    list.add(InstructionFactory.createBranchLink(printLabel));
    InstructionList printHelperFunction = PrintFunctions.printLn(data);
    helperFunctions.add(printHelperFunction);
  }

  @Override
  public InstructionList visitEqualityOper(WACCParser.EqualityOperContext ctx) {
    InstructionList list = defaultResult();
    Register dst1 = accMachine.peekFreeRegister();
    list.add(visitAddOper(ctx.first));
    if (ctx.second != null) {
      Register dst2 = accMachine.peekFreeRegister();

      long trueLong = (ctx.EQ() != null) ? 1 : 0;
      long falseLong = (ctx.EQ() != null) ? 0 : 1;
      Operand trueOp = new Immediate(trueLong);
      Operand falseOp = new Immediate(falseLong);
      list.add(visitAddOper(ctx.second))
          .add(InstructionFactory.createCompare(dst1, dst2))
          .add(InstructionFactory.createMovEq(dst1, trueOp))
          .add(InstructionFactory.createMovNe(dst1, falseOp));
      accMachine.pushFreeRegister(dst2);
    }

    return list;
  }

  @Override
  public InstructionList visitAddOper(WACCParser.AddOperContext ctx) {
    InstructionList list = defaultResult();
    Register dst1 = accMachine.peekFreeRegister();
    list.add(visitMultOper(ctx.first));
    if (!ctx.otherExprs.isEmpty()) {
      Register dst2;
      // for loop used instead of visitChildren so only 2 registers used up
      for (int i = 0; i < ctx.ops.size(); i++) {
        InstructionList arithmeticInstr = defaultResult();
        WACCParser.MultOperContext otherExpr = ctx.otherExprs.get(i);
        dst2 = accMachine.peekFreeRegister();
        String op = ctx.ops.get(i).getText();

        list.add(visitMultOper(otherExpr));

        if (op.equals(getToken(WACCParser.PLUS))){
          arithmeticInstr.add(accMachine.getInstructionList(InstructionType.ADDS, dst1, dst1, dst2));
        } else {
          arithmeticInstr.add(accMachine.getInstructionList(InstructionType.SUBS, dst1, dst1, dst2));
        }

        // TODO: Remember, the other expression must be visited before the arithmetic op instruction is created
        list.add(arithmeticInstr);
        accMachine.pushFreeRegister(dst2);
      }
    }

    return list;
  }

  @Override
  public InstructionList visitMultOper(WACCParser.MultOperContext ctx) {
    InstructionList list = defaultResult();
    Register dst1 = accMachine.peekFreeRegister();
    list.add(visitAtom(ctx.first));
    if (!ctx.otherExprs.isEmpty()) {
      Register dst2;
      // for loop used instead of visitChildren so only 2 registers used up
      for (int i = 0; i < ctx.ops.size(); i++) {
        InstructionList arithmeticInstr = defaultResult();
        WACCParser.AtomContext otherExpr = ctx.otherExprs.get(i);
        dst2 = accMachine.peekFreeRegister();
        String op = ctx.ops.get(i).getText();

        if (op.equals(getToken(WACCParser.MUL))){
          // TODO: check this is right for all cases
          // TODO: if always dst1, dst2,dst1,dst2 that is thee MOST pointless
          // argument list since the 1940's
          arithmeticInstr.add(InstructionFactory.createSmull(dst1,
              dst2,
              dst1,
              dst2));
        } else if (op.equals(getToken(WACCParser.DIV))){
          arithmeticInstr.add(divMoves(dst1, dst2))
              .add(InstructionFactory.createDiv())
              .add(InstructionFactory.createMov(dst1, ARM11Registers.R0));
        } else {
          arithmeticInstr.add(divMoves(dst1, dst2))
              .add(InstructionFactory.createMod())
              .add(InstructionFactory.createMov(dst1, ARM11Registers.R1));
        }

        list.add(visitAtom(otherExpr))
            .add(arithmeticInstr);
        accMachine.pushFreeRegister(dst2);
      }
    }

    return list;
  }

  private InstructionList divMoves(Operand dst1, Operand dst2) {
    return defaultResult()
        .add(InstructionFactory.createMov(ARM11Registers.R0, dst1))
        .add(InstructionFactory.createMov(ARM11Registers.R1, dst2));
  }

  @Override
  public InstructionList visitUnaryOper(WACCParser.UnaryOperContext ctx) {
    InstructionList list = defaultResult();
    Register dst = accMachine.peekFreeRegister();
    if (ctx.ident() != null) {
      list.add(visitIdent(ctx.ident()));
    } else if (ctx.expr() != null) {
      list.add(visitExpr(ctx.expr()));
    }

    if (ctx.NOT() != null) {
      list.add(InstructionFactory.createEOR(dst, dst, new Immediate(1L)));
    } else if (ctx.MINUS() != null) {
      list.add(InstructionFactory.createRSBS(dst, dst, new Immediate(0L)));
    } else if (ctx.LEN() != null) {
      list.add(InstructionFactory.createLoad(dst, dst, new Immediate(0L)));
    }

    return list;
  }

  @Override
  public InstructionList visitInteger(WACCParser.IntegerContext ctx) {
    InstructionList list = defaultResult();

    Operand op;
    InstructionList loadOrMove = defaultResult();
    Register reg = accMachine.popFreeRegister();
    String digits = ctx.INTEGER().getText();
    long value = Long.parseLong(digits);

    if (ctx.CHR() != null){
      String chr = "\'" + (char) ((int) value) + "\'";
      op = new Immediate(chr);
      loadOrMove.add(InstructionFactory.createMov(reg, op));
    } else {
      op = new Immediate(value);
      loadOrMove.add(accMachine.getInstructionList(InstructionType.LDR, reg, op));
    }

    return list.add(loadOrMove);
  }

  @Override
  public InstructionList visitCall(WACCParser.CallContext ctx) {
    InstructionList list = defaultResult();
    String functionName = ScopeType.FUNCTION_SCOPE + ctx.funcName.getText();
    Label functionLabel = new Label(functionName);

    if (ctx.argList() != null) {
      list.add(visitArgList(ctx.argList()));
    }
    list.add(InstructionFactory.createBranchLink(functionLabel));
    Register result = accMachine.popFreeRegister();
    if (ctx.argList() != null) {
      Operand size = new Immediate(totalListSize(ctx.argList().expr()));
      list.add(InstructionFactory.createAdd(ARM11Registers.SP,
          ARM11Registers.SP, size));
    }
    list.add(InstructionFactory.createMov(result, ARM11Registers.R0));

    return list;
  }

  private Long totalListSize(List<? extends WACCParser.ExprContext> exprs) {
    Long totalSize = 0L;
    for (WACCParser.ExprContext exprCtx : exprs) {
      totalSize += exprCtx.returnType.getSize();
    }
    return totalSize;
  }

  @Override
  public InstructionList visitArgList(WACCParser.ArgListContext ctx) {
    InstructionList list = defaultResult();

    for (int i = ctx.expr().size() - 1; i >= 0; i--) {
      WACCParser.ExprContext exprCtx = ctx.expr(i);
      Register result = accMachine.peekFreeRegister();
      Long varSize = (long) -exprCtx.returnType.getSize();
      Operand size = new Immediate(varSize);
      list.add(visitExpr(exprCtx));
      if (varSize == -1L) {
        list.add(InstructionFactory.createStoreByte(result, ARM11Registers.SP,
                                                    size));
      } else {
        list.add(InstructionFactory.createStore(result, ARM11Registers.SP, size));
      }
      accMachine.pushFreeRegister(result);
    }
    // Put back in the correct order!!
    return list;
  }

  @Override
  public InstructionList visitBool(WACCParser.BoolContext ctx) {
    InstructionList list = defaultResult();

    Operand op;
    Instruction move;
    Register reg = accMachine.popFreeRegister();

    String boolLitr = ctx.boolLitr().getText();
    long value = boolLitr.equals("false") ? 0 : 1;

    if (ctx.NOT() != null){
      // ^ is XOR
      op = new Immediate(value^1);
    } else {
      op = new Immediate(value);
    }
    move = InstructionFactory.createMov(reg, op);

    return list.add(move);
  }

  @Override
  public InstructionList visitCharacter(WACCParser.CharacterContext ctx) {
    InstructionList list = defaultResult();

    Operand op;
    Instruction loadOrMove;
    Register reg = accMachine.popFreeRegister();
    String chr = ctx.CHARACTER().getText();

    if (ctx.ORD() != null){
      long value = (int) chr.charAt(1);
      op = new Immediate(value);
      loadOrMove = InstructionFactory.createLoad(reg, op);
    } else {
      op = new Immediate(chr);
      loadOrMove = InstructionFactory.createMov(reg, op);
    }

    return list.add(loadOrMove);
  }

  @Override
  public InstructionList visitString(WACCParser.StringContext ctx) {
    InstructionList list = defaultResult();

    Operand op;
    Register reg = accMachine.popFreeRegister();
    String text = ctx.STRING().getText();
    op = data.addConstString(text);
    list.add(InstructionFactory.createLoad(reg, op));

    if (ctx.LEN() != null) {
      // TODO: allow for 0 default offset
      list.add(InstructionFactory.createLoad(reg, reg, new Immediate(0L)));
    }
    return list;
  }

  @Override
  public InstructionList visitIdent(WACCParser.IdentContext ctx) {
    InstructionList list = defaultResult();
    Variable variable = getMostRecentBindingForVariable(ctx.getText());
    
    Immediate offset 
        = new Immediate(getAccumulativeOffsetForVariable(ctx.getText()));
    Register reg = accMachine.popFreeRegister();
    Register sp = ARM11Registers.SP;

      if (Type.isBool(variable.getType()) || Type.isChar(variable.getType())) {
        list.add(InstructionFactory.createLoadStoredByte(reg, sp, offset));
      } else {
        list.add(accMachine.getInstructionList(
            InstructionType.LDR, reg, sp, offset));
      }

    return list;
  }


  @Override
  public InstructionList visitFreeStat(WACCParser.FreeStatContext ctx) {
    InstructionList list = defaultResult();
    list.add(visitExpr(ctx.expr()));
    list.add(InstructionFactory.createBranchLink(new Label("p_free_pair")));

    helperFunctions.add(freePair(data));
    return list;
  }

  @Override
  public InstructionList visitNewPair(WACCParser.NewPairContext ctx) {
    InstructionList list = defaultResult();
    Label malloc = new Label("malloc");
    Register result = accMachine.popFreeRegister();
    Operand sizeOfObject = new Immediate(PAIR_SIZE);

    list.add(InstructionFactory.createLoad(ARM11Registers.R0, sizeOfObject));
    list.add(InstructionFactory.createBranchLink(malloc));
    list.add(InstructionFactory.createMov(result, ARM11Registers.R0));

    Long accSize = 0L;
    for (WACCParser.ExprContext exprCtx : ctx.expr()) {
      Long size = (long) exprCtx.returnType.getSize();
      Register next = accMachine.peekFreeRegister();
      list.add(visitExpr(exprCtx));

      list.add(InstructionFactory.createLoad(ARM11Registers.R0,
                                             new Immediate(size)));
      list.add(InstructionFactory.createBranchLink(malloc));
      list.add(InstructionFactory.createStore(next, ARM11Registers.R0,
                                              new Immediate(0L)));
      accMachine.pushFreeRegister(next);
      list.add(InstructionFactory.createStore(ARM11Registers.R0,
                                              result,
                                              new Immediate(accSize)));
      accSize += size;
    }

    return list;
  }

  @Override
  public InstructionList visitBeginStat(WACCParser.BeginStatContext ctx) {
    ++beginCount;
    InstructionList list = defaultResult();

    String beginScope = Scope.BEGIN.toString() + beginCount;
    changeWorkingSymbolTableTo(beginScope);
    pushEmptyVariableSet();

    list.add(allocateSpaceOnStack())
            .add(visitStatList(ctx.statList()))
            .add(deallocateSpaceOnStack());

    popCurrentScopeVariableSet();
    goUpWorkingSymbolTable();

    return list;
  }

  @Override
  public InstructionList visitReadStat(WACCParser.ReadStatContext ctx) {
    InstructionList list = defaultResult();

    Register reg = accMachine.popFreeRegister();
    Register dst = ARM11Registers.R0;

    Immediate imm = new Immediate((long) 0);
    InstructionList printHelperFunction;
    Label readLabel;

    if (Type.isInt((Type) ctx.assignLHS().returnType)) {
      readLabel = new Label("p_read_int");
      printHelperFunction = ReadFunctions.readInt(data);
    } else {
      readLabel = new Label("p_read_char");
      printHelperFunction = ReadFunctions.readChar(data);
    }

    list.add(InstructionFactory.createAdd(reg, ARM11Registers.SP, imm))
        .add(InstructionFactory.createMov(dst, reg))
        .add(InstructionFactory.createBranchLink(readLabel));
    accMachine.pushFreeRegister(reg);
    helperFunctions.add(printHelperFunction);

    return list;
  }

  @Override
  public InstructionList visitPairLitr(WACCParser.PairLitrContext ctx) {
    Register result = accMachine.popFreeRegister();
    Operand nullOp = new Immediate(0L);
    return defaultResult().add(InstructionFactory.createLoad(result, nullOp));
  }

  @Override
  public InstructionList visitPairElem(@NotNull WACCParser.PairElemContext ctx) {
    InstructionList list = defaultResult();

    return list;
  }

  @Override
  public InstructionList visitArrayLitr(WACCParser.ArrayLitrContext ctx) {

    InstructionList list = defaultResult();

    long bytesToAllocate = ADDRESS_SIZE;
    long typeSize = 0;

    long numberOfElems = ctx.expr().size();
    if (numberOfElems != 0) {
      Type returnType = ctx.expr().get(0).returnType;
      typeSize = returnType.getSize();
      bytesToAllocate += typeSize * numberOfElems;
    }

    list.add(InstructionFactory.createLoad(ARM11Registers.R0,
            new Immediate(bytesToAllocate)));
    Label malloc = new Label("malloc");
    list.add(InstructionFactory.createBranchLink(malloc));
    
    Register addressOfArray = accMachine.popFreeRegister();
    list.add(InstructionFactory.createMov(addressOfArray, ARM11Registers.R0));

    // Load all the exprs into the array (if any)
    long offset = 4;
    for (WACCParser.ExprContext elem : ctx.expr()) {
      Register result = accMachine.peekFreeRegister();
      list.add(visitExpr(elem))
              .add(InstructionFactory.createStore(result,
                      addressOfArray,
                      new Immediate(offset)));
      offset += typeSize;
      accMachine.pushFreeRegister(result);
    }
    
    Register lengthOfArray = accMachine.peekFreeRegister();

    list.add(InstructionFactory.createLoad(lengthOfArray,
            new Immediate(numberOfElems)))
        .add(InstructionFactory.createStore(lengthOfArray,
                addressOfArray,
                new Immediate((long) 0)));

    accMachine.pushFreeRegister(addressOfArray);

    return list;
  }

  @Override
  public InstructionList visitParam(@NotNull WACCParser.ParamContext ctx) {
    String name = ctx.name.getText();
    addVariableToCurrentScope(name);
    // set the variable as a param
    Variable var = (Variable) workingSymbolTable.lookupAll(name);
    var.setAsParam();
    return null;
  }

  @Override
  public InstructionList visitFunc(WACCParser.FuncContext ctx) {
    InstructionList list = defaultResult();
    changeWorkingSymbolTableTo(ScopeType.FUNCTION_SCOPE
            + ctx.funcName.getText());
    pushEmptyVariableSet();

    Label functionLabel = new Label(ScopeType.FUNCTION_SCOPE
            + ctx.funcName.getText());

    if (ctx.paramList() != null) {
      visitParamList(ctx.paramList());
    }

    list.add(InstructionFactory.createLabel(functionLabel));
    list.add(InstructionFactory.createPush(ARM11Registers.LR))
            .add(allocateSpaceOnStack())
            .add(visitStatList(ctx.statList()))
            .add(deallocateSpaceOnStack())
            .add(InstructionFactory.createPop(ARM11Registers.PC))
            .add(InstructionFactory.createPop(ARM11Registers.PC))
            .add(InstructionFactory.createLTORG());
    popCurrentScopeVariableSet();
    goUpWorkingSymbolTable();

    return list;
  }
}
