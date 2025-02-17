package bindings;

public class ArrayType extends Type {

  private Type base;
  private int dimensionality;
  private boolean generic = true;

  public ArrayType() {
    super(Types.GENERIC_ARRAY_T);
  }

  public ArrayType(Type base) {
    // Created array from type
    // T -> T[], or T[] -> T[][]

    super(base.getName());
    generic = false;
    this.dimensionality = 1;
    if (base instanceof ArrayType) {
      this.dimensionality += ((ArrayType) base).getDimensionality();
      this.base = ((ArrayType) base).base;
    } else {
      this.base = base;
    }
  }

  public ArrayType(Type base, int dimensionality) {
    super(base.getName());
    assert dimensionality > 0 : "Dimensionality must be greater than 0";
    generic = false;
    this.dimensionality = dimensionality;
    this.base = base;
  }

  public static boolean isArray(Type type) {
    if (type == null) {
      return false;
    }
    return ((type instanceof ArrayType) || Type.isString(type));
  }

  public static boolean isCharArray(Type type) {
    if (type == null) {
      return false;
    }
    return (type instanceof ArrayType) && Type.isChar(((ArrayType) type).base);
  }

  @Override
  public String toString() {

    if (generic) {
      return super.toString();
    }

    String brackets = new String(new char[dimensionality]).replace("\0", "[]");
    return base.toString() + brackets;
  }

  @Override
  public String toLabel() {
    StringBuilder sb = new StringBuilder(base.toLabel());
    for (int i = 1; i <= dimensionality; i++) {
      sb.append(TypeLabels.ARRAY_L);
    }
    return sb.toString();
  }

  public int getDimensionality() {
    return dimensionality;
  }

  public Type getBase() {
    return base;
  }

  public static Type createArray(Type base, int dimensionality) {
    if (dimensionality == 0) {
      return base;
    } else {
      return new ArrayType(base, dimensionality);
    }
  }

  @Override
  public int getSize() {
    return SIZE_OF_ADDRESS;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      //Check if current array is a char array && object is a string
      return isCharArray(this)
          && o instanceof Type
          && Type.isString((Type) o);
    }

    ArrayType arrayType = (ArrayType) o;

    if(arrayType.generic || generic) {
      return true;
    }

    if (arrayType.base.equals(this.base)) {
      return true;
    }

    return super.equals(o);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (base != null ? base.hashCode() : 0);
    result = 31 * result + dimensionality;
    return result;
  }
}
