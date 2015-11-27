package arm11;

public class PrintFunctions {

  private static void printAsciiAndFlush(InstructionList list) {
    list.add(InstructionFactory.createBranchLink(new Label("puts")))
        .add(InstructionFactory.createMov(ARM11Registers.R0,
                                          new Immediate((long) 0)))
        .add(InstructionFactory.createBranchLink(new Label("fflush")));
  }

  private static void saveLinkRegister(InstructionList list,
                                       Label printLabel) {
    list.add(InstructionFactory.createLabel(printLabel))
        .add(InstructionFactory.createPush(ARM11Registers.LR));
  }

  public static InstructionList printInt(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label printLabel = new Label("p_print_int");
    Label intFormatterLabel
        = data.addPrintFormatter(IOFormatters.INT_FORMATTER);

    saveLinkRegister(list, printLabel);
    list.add(InstructionFactory.createMov(ARM11Registers.R1,
                                          ARM11Registers.R0))
        .add(InstructionFactory.createLoad(ARM11Registers.R0,
                                           intFormatterLabel))
        .add(InstructionFactory.createAdd(ARM11Registers.R0,
                                          ARM11Registers.R0,
                                          new Immediate((long) 4)));
    printAsciiAndFlush(list);
    list.add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

  public static InstructionList printString(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label printLabel = new Label("p_print_string");
    Label stringFormatterLabel
        = data.addPrintFormatter(IOFormatters.STRING_FORMATTER);

    saveLinkRegister(list, printLabel);
    list.add(InstructionFactory.createLoad(ARM11Registers.R1,
                                           new Address(ARM11Registers.R0)))
        .add(InstructionFactory.createAdd(ARM11Registers.R2,
                                          ARM11Registers.R0,
                                          new Immediate((long) 4)))
        .add(InstructionFactory.createLoad(ARM11Registers.R0,
                                           stringFormatterLabel))
        .add(InstructionFactory.createAdd(ARM11Registers.R0,
                                          ARM11Registers.R0,
                                          new Immediate((long) 4)));
    printAsciiAndFlush(list);
    list.add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

  public static InstructionList printLn(DataInstructions data) {
    InstructionList list = new InstructionList();
    Label printLabel = new Label("p_print_ln");
    Label lnFormatterLabel = data.addPrintFormatter(IOFormatters.LN_FORMATTER);

    saveLinkRegister(list, printLabel);
    list.add(InstructionFactory.createLoad(ARM11Registers.R0,
                                           lnFormatterLabel))
        .add(InstructionFactory.createAdd(ARM11Registers.R0,
                                          ARM11Registers.R0,
                                          new Immediate((long) 4)));
    printAsciiAndFlush(list);
    list.add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

  public static InstructionList printBool(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label printLabel = new Label("p_print_bool");
    Label trueFormatterLabel
        = data.addPrintFormatter(IOFormatters.BOOL_TRUE_FORMATTER);
    Label falseFormatterLabel
        = data.addPrintFormatter(IOFormatters.BOOL_FALSE_FORMATTER);

    saveLinkRegister(list, printLabel);
    list.add(InstructionFactory.createCompare(ARM11Registers.R0,
                                              new Immediate((long) 0)))
        .add(InstructionFactory.createLoadNotEqual(ARM11Registers.R0,
                                                   trueFormatterLabel))
        .add(InstructionFactory.createLoadEqual(ARM11Registers.R0,
                                                falseFormatterLabel))
        .add(InstructionFactory.createAdd(ARM11Registers.R0,
                                          ARM11Registers.R0,
                                          new Immediate((long) 4)));
    printAsciiAndFlush(list);
    list.add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

  public static InstructionList printReference(DataInstructions data) {
    InstructionList list = new InstructionList();

    Label printLabel = new Label("p_print_reference");
    Label referenceFormatterLabel
        = data.addPrintFormatter(IOFormatters.REFERENCE_FORMATTER);

    saveLinkRegister(list, printLabel);
    list.add(InstructionFactory.createLoad(ARM11Registers.R1,
                                           ARM11Registers.R0))
        .add(InstructionFactory.createLoad(ARM11Registers.R0,
                                           referenceFormatterLabel))
        .add(InstructionFactory.createAdd(ARM11Registers.R0,
                                          ARM11Registers.R0,
                                          new Immediate((long) 4)));
    printAsciiAndFlush(list);
    list.add(InstructionFactory.createPop(ARM11Registers.PC));

    return list;
  }

}