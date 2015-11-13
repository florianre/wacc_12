import bindings.*;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TypeTest {

  @Test
  public void testIntType() {
    Type intType = new Type(Types.INT_T.toString());
    assertThat(intType.toString(), is("int"));
  }

  @Test
  public void testBoolType() {
    Type intType = new Type(Types.BOOL_T.toString());
    assertThat(intType.toString(), is("bool"));
  }

  @Test
  public void testCharType() {
    Type intType = new Type(Types.CHAR_T.toString());
    assertThat(intType.toString(), is("char"));
  }

  @Test
  public void testStringType() {
    Type intType = new Type(Types.STRING_T.toString());
    assertThat(intType.toString(), is("string"));
  }

  @Test
  public void testBaseArrayType() {
    Type baseType = new Type(Types.INT_T.toString());
    ArrayType arrayType = new ArrayType(baseType);
    assertThat(arrayType.toString(), is("int[]"));
  }

  @Test
  public void testMultiArrayType() {
    Type baseType = new Type(Types.INT_T.toString());
    ArrayType innerArray = new ArrayType(baseType);
    ArrayType outerArray = new ArrayType(innerArray);
    assertThat(outerArray.toString(), is("int[][]"));
  }

  @Test
  public void testPairType() {
    Type fstType = new Type(Types.INT_T.toString());
    Type sndType = new Type(Types.STRING_T.toString());
    PairType pairType = new PairType(fstType, sndType);
    assertThat(pairType.toString(), is("pair(int, string)"));
  }

  @Test
  public void testPairLitrPairType() {
    Type pairLitr = new Type(Types.PAIR_T.toString());
    Type baseType = new Type(Types.INT_T.toString());
    PairType pairType = new PairType(baseType, pairLitr);
    assertThat(pairType.toString(), is("pair(int, pair)"));
  }

  @Test
  public void testPairPairType() {
    Type fstType = new Type(Types.INT_T.toString());
    Type sndType = new Type(Types.STRING_T.toString());

    PairType fstInnerPair
        = new PairType(fstType, sndType);
    PairType sndInnerPair
        = new PairType(sndType, fstType);

    PairType outerPair
        = new PairType(fstInnerPair, sndInnerPair);

    String type = "pair(pair, pair)";
    assertThat(outerPair.toString(), is(type));
  }

  @Test
  public void testPairOfArrays() {
    Type fstBaseType = new Type(Types.INT_T.toString());
    Type sndBaseType = new Type(Types.CHAR_T.toString());
    ArrayType fstArrayType = new ArrayType(fstBaseType);
    ArrayType sndArrayType = new ArrayType(sndBaseType);
    PairType pairType = new PairType(fstArrayType, sndArrayType);
    String type = "pair(int[], char[])";
    assertThat(pairType.toString(), is(type));
  }

  @Test
  public void testArrayOfPairs() {
    Type fstBaseType = new Type(Types.INT_T.toString());
    Type sndBaseType = new Type(Types.CHAR_T.toString());
    PairType pairType = new PairType(fstBaseType, sndBaseType);
    ArrayType arrayType = new ArrayType(pairType);
    String type = "pair(int, char)[]";
    assertThat(arrayType.toString(), is(type));
  }

}
