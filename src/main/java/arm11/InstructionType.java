package arm11;

public enum InstructionType {
  LDR("LDR"),
  BL("BL"),
  LABEL("label"),
  PUSH("PUSH"),
  POP("POP"),
  LTORG(".ltorg"),
  TEXT(".text"),
  GLOBAL(".global"),
  SUB("SUB"),
  ADD("ADD"),
  MOV("MOV"),
  WORD(".word"),
  ASCII(".ascii"),
  CMP("CMP"),
  LDRNE("LDRNE"),
  LDREQ("LDREQ"),
  STR("STR");

  private final String name;

  InstructionType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "\t" + this.name;
  }
}
