package arm11;

public class RuntimeErrorFunctions {

  // TODO: make errors constants

  public static InstructionList divideByZero(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label checkDivideByZerolabel = new Label("p_check_divide_by_zero");
    Label throwRuntimeErrorLabel = new Label("p_throw_runtime_error");
    Label errMessage = data.addMessage("\"DivideByZeroError: divide or "
                                           + "modulo by zero\\n\\0\"");

    list.add(InstructionFactory.createLabel(checkDivideByZerolabel))
        .add(InstructionFactory.createPush(ARM11Registers.LR))
        .add(InstructionFactory.createCompare(ARM11Registers.R1,
                                              new Immediate(0L)))
        .add(InstructionFactory.createLoadEqual(ARM11Registers.R0, errMessage))
        .add(InstructionFactory.createBranchLinkEqual(throwRuntimeErrorLabel))
        .add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

  public static InstructionList overflowError(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label throwOverFlowErrorLabel = new Label("p_throw_overflow_error");
    Label throwRuntimeErrorLabel = new Label("p_throw_runtime_error");
    Label errMessage = data.addMessage("\"OverflowError: the result is too "
                                           + "small/large to store in a 4-byte "
                                           + "signed-integer.\\n\"");

    list.add(InstructionFactory.createLabel(throwOverFlowErrorLabel))
        .add(InstructionFactory.createLoad(ARM11Registers.R0, errMessage))
        .add(InstructionFactory.createBranchLink(throwRuntimeErrorLabel));

    return list;
  }

  public static InstructionList checkArrayBounds(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label checkArrayBoundsLabel = new Label("p_check_array_bounds");
    Label throwRuntimeErrorLabel = new Label("p_throw_runtime_error");
    Label negErrMessage
      = data.addMessage("\"ArrayIndexOutOfBoundsError: negative index\\n\\0\"");
    Label oufOfBoundIndexErrMessage
      = data.addMessage("\"ArrayIndexOutOfBoundsError: index too "
                            + "large\\n\\0\"");

    list.add(InstructionFactory.createLabel(checkArrayBoundsLabel))
        .add(InstructionFactory.createPush(ARM11Registers.LR))
        .add(InstructionFactory.createCompare(ARM11Registers.R0,
                                              new Immediate(0L)))
        .add(InstructionFactory.createLoadLessThan(ARM11Registers.R0,
                                                   negErrMessage))
        .add(InstructionFactory.createBranchLinkLT(throwRuntimeErrorLabel))
        .add(InstructionFactory.createLoad(ARM11Registers.R1,
                                           new Address(ARM11Registers.R1)))
        .add(InstructionFactory.createCompare(ARM11Registers.R0,
                                              ARM11Registers.R1))
        .add(InstructionFactory.createLoadCS(ARM11Registers.R0,
                                             oufOfBoundIndexErrMessage))
        .add(InstructionFactory.createBranchLinkCS(throwRuntimeErrorLabel))
        .add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

  public static InstructionList throwRuntimeError(DataInstructions data) {
    InstructionList list = new InstructionList();
    data.addPrintFormatter(IOFormatters.STRING_FORMATTER);

    Immediate exitCode = new Immediate(-1L);

    list.add(InstructionFactory.createLabel(new Label("p_throw_runtime_error")))
         .add(InstructionFactory.createBranchLink(new Label("p_print_string")))
        .add(InstructionFactory.createMove(ARM11Registers.R0, exitCode))
        .add(InstructionFactory.createBranchLink(new Label("exit")));

    return list;
  }

  public static InstructionList checkNullPointer(DataInstructions data) {
    InstructionList list = new InstructionList();
    data.addPrintFormatter(IOFormatters.STRING_FORMATTER);

    Label throwRuntimeError = new Label("p_throw_runtime_error");
    Label checkNullPointer = new Label("p_check_null_pointer");
    Label errMessage = data.addMessage("\"NullReferenceError: dereference a "
                                       + "null reference\\n\\0\"");

    list.add(InstructionFactory.createLabel(checkNullPointer))
        .add(InstructionFactory.createPush(ARM11Registers.LR))
        .add(InstructionFactory.createCompare(ARM11Registers.R0,
                                              new Immediate(0L)))
        .add(InstructionFactory.createLoadEqual(ARM11Registers.R0, errMessage))
        .add(InstructionFactory.createBranchEqual(throwRuntimeError))
        .add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

}
