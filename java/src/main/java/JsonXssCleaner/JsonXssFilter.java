package JsonXssCleaner;

import java.util.Iterator;

import org.parboiled.BaseParser;
import org.parboiled.Node;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SkipNode;
import org.parboiled.buffers.InputBuffer;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

@BuildParseTree
public class JsonXssFilter extends BaseParser<Object> {
  // the root rule
  Rule Json() {
    // PUSH 1<AstNode>
    return Sequence(WhiteSpace(), Optional(FirstOf(JsonObject(), JsonArray()).skipNode()).skipNode(), WhiteSpace(), EOI).label(
        "Root");
    // {WhiteSpace ~ (JsonObject | JsonArray) ~ EOI }
  }

  @SkipNode
  Rule JsonObject() {
    // PUSH 1<ObjectNode>
    // System.out.println("JsonObject:");
    return Sequence(String("{"), WhiteSpace(), ZeroOrMore(Pair()).skipNode(),
        ZeroOrMore(Sequence(WhiteSpace(), String(","), WhiteSpace(), Pair()).skipNode()).skipNode(), WhiteSpace(), String("}"));

    // "{ " ~ zeroOrMore(Pair, separator = ", ") ~ "} " ~~> ObjectNode
  }

  @SkipNode
  Rule Pair() {
    // PUSH 1<MemberNode>
    return Sequence(WhiteSpace(), JsonString(), WhiteSpace(), String(":"), WhiteSpace(), Value(), WhiteSpace());

    // JsonString ~~> (_.text) ~ ": " ~ Value ~~> MemberNode
  }

  Rule Value() {
    // PUSH 1<AstNode>
    return FirstOf(JsonString(), JsonNumber(), JsonTrue(), JsonFalse(), JsonNull(), JsonObject(), JsonArray()).skipNode();
    /*
     * , ,
     * 
     * )
     */
    // JsonString | JsonNumber | JsonObject | JsonArray | JsonTrue | JsonFalse |
    // JsonNull
  }

  Rule String() {
    return ZeroOrMore(Character());
  }

  @SkipNode
  Rule JsonString() {
    // PUSH
    return Sequence(String("\""), ZeroOrMore(Character()).skipNode(), String("\""));
    // "\"" ~ zeroOrMore(Character) ~> StringNode ~ "\" "
  }

  Rule JsonNumber() {
    // PUSH 1<NumberNode>
    return FirstOf(HexInteger(), Float(), Integer());
  }

  @SkipNode
  Rule Float() {
    return Sequence(Integer(), FirstOf(Sequence(Frac(), Exp()).skipNode(), Exp(), Frac()).skipNode());
  }

  @SkipNode
  Rule HexInteger() {
    return Sequence(FirstOf(String("0x").skipNode(), String("-0x").skipNode()).skipNode(), OneOrMore(HexDigit()).skipNode());
  }

  @SkipNode
  Rule JsonArray() {
    // PUSH 1<ArrayNode>
    // System.out.println("JsonArray:");
    return Sequence(String("["), WhiteSpace(), ZeroOrMore(Value()).skipNode(),
        ZeroOrMore(Sequence(WhiteSpace(), String(","), WhiteSpace(), Value()).skipNode()).skipNode(), WhiteSpace(), String("]"));
    /*
     * return Sequence(String("["), ZeroOrMore(Value())
     * 
     * , Optional(String(","), Value())String("]");
     */
    // "[ " ~ zeroOrMore(Value, separator = ", ") ~ "] " ~~> ArrayNode
  }

  @SkipNode
  Rule Character() {
    return FirstOf(subLess(), subMore(), subQuote(), subExecuteQuote(), subDoubleQuote(), EscapedChar(), NormalChar());
  }

  // @SkipNode
  Rule subLess() {
    return Sequence(FirstOf(String("<").skipNode(), IgnoreCase("\\u003c").skipNode()).skipNode(), push(new ValueNode("&lt;")));
  }

  Rule subMore() {
    return Sequence(FirstOf(String(">").skipNode(), IgnoreCase("\\u003e").skipNode()).skipNode(), push(new ValueNode("&gt;")));
  }

  Rule subQuote() {
    return Sequence(FirstOf(String("'").skipNode(), IgnoreCase("\\u0027").skipNode()).skipNode(), push(new ValueNode("&apos;")));
  }

  Rule subExecuteQuote() {
    return Sequence(FirstOf(String("`").skipNode(), IgnoreCase("\\u0060").skipNode()).skipNode(), push(new ValueNode("&#96;")));
  }

  Rule subDoubleQuote() {
    return Sequence(FirstOf(String("\\\"").skipNode(), IgnoreCase("\\\\u0022").skipNode()).skipNode(),
        push(new ValueNode("&quot;")));
  }

  Rule EscapedChar() {
    return Sequence(String("\\").skipNode(), FirstOf(AnyOf("\"\\/bfnrt").skipNode(), Unicode()).skipNode());
    // "\\" ~ (anyOf("\"\\/bfnrt") | Unicode)
  }

  Rule NormalChar() {
    return Sequence(TestNot(AnyOf("\"\\").skipNode()), ANY.skipNode());
  }

  @SkipNode
  Rule Unicode() {
    return Sequence(String("u").skipNode(), HexDigit(), HexDigit(), HexDigit(), HexDigit());
    // "u" ~ HexDigit ~ HexDigit ~ HexDigit ~ HexDigit
  }

  @SkipNode
  Rule Integer() {
    // return String("9");
    return Sequence(Optional(AnyOf("+-").skipNode()).skipNode(), Digit(), Optional(Digits()).skipNode());

    /*
     * , swap3 ( ) , push ( popString ( ) + popString ( ) + popString ( ) )
     */
    // optional("-") ~ ("1" - "9" ~ Digits | Digit)
  }

  @SkipNode
  Rule Digits() {
    return OneOrMore(Digit());
    // oneOrMore(this.Digit)
  }

  @SkipNode
  Rule Digit() {
    return CharRange('0', '9');
  }

  @SkipNode
  Rule HexDigit() {
    return FirstOf(Digit(), CharRange('a', 'f').skipNode(), CharRange('A', 'F').skipNode());
    // "0" - "9" | "a" - "f" | "A" - "Z"
  }

  @SkipNode
  Rule Frac() {
    return Sequence(String(".").skipNode(), Digits());
  }

  @SkipNode
  Rule Exp() {
    return Sequence(IgnoreCase("e").skipNode(), Integer());
  }

  Rule JsonTrue() {
    // PUSH
    return String("true");
    // "true " ~ push(True)
  }

  Rule JsonFalse() {
    // PUSH
    return String("false");
    // "false " ~ push(False)
  }

  Rule JsonNull() {
    // PUSH
    return String("null");
    // "null " ~ push(Null)
  }

  @SkipNode
  Rule WhiteSpace() {
    return ZeroOrMore(AnyOf(" \n\r\t\f").skipNode());
    // zeroOrMore(anyOf())
  }

  private static JsonXssFilter parser = Parboiled.createParser(JsonXssFilter.class);

  private static String clean(final String inp, final Rule root) {

    ParsingResult<ValueNode> result = new RecoveringParseRunner<ValueNode>(root).run(inp);
    if (!result.matched || !result.parseErrors.isEmpty()) {
      return null;
    }

    // String parseTreePrintOut = ParseTreeUtils.printNodeTree(result);
    // System.out.println(parseTreePrintOut);
    return traverse(result.parseTreeRoot, result.inputBuffer, null).toString();
  }

  public static String cleanJson(final String inp) {
    return clean(inp, parser.Json());
  }

  public static String cleanString(final String inp) {
    return clean(inp, parser.String());
  }

  /**
   * The main parsing method. Uses a ReportingParseRunner (which only reports
   * the first error) for simplicity.
   * 
   * @return
   */
  public static void main(final String[] argv) {

    String inp = " [9,\"0xaffe meno\", \"[\\t\\b\\f\\n\\r\\\\\\\"\\uaffe ]\", true,false ,   null, 0x10, -0x10, -9, 8.7, -8.7, 8e+10,8e10,8e-10, "
        + "-8e+10,-8e10,-8e-10, 8.8e+10,8.8e10,8.9e-10, -8.8e+10,-8.8e10,-8.9e-10,[1,2],{}]";
    inp = "{\"meno\": 42.9, \"abels\":\"<script>\", \"1\": " + inp + "}";
    // inp = "[+412,\"m<  > \\u003c en\\\" ja \"]"; //
    System.out.println("Hello Json:" + inp);
    System.out.println("Ciao  Json:" + cleanJson(inp));
    System.out.println("Ciao  String:" + cleanString("meno<script>ja"));

  }

  private static StringBuilder traverse(final Node<ValueNode> o, final InputBuffer inp, final StringBuilder _sb) {

    // if (o.getValue() != null/* && o.getLabel() == "Value" */) {
    StringBuilder sb = _sb;
    if (sb == null) {
      sb = new StringBuilder();
    }
    if (_sb != null) {
      if (o.getValue() != null && o.getLabel().startsWith("sub")) {
        // System.out.println(o.getLabel() + ":" + o.getValue().getValue());
        sb.append(o.getValue().getValue());
      } else {
        String str = inp.extract(o.getStartIndex(), o.getEndIndex());
        sb.append(str);
      }
    }
    // }
    Iterator<Node<ValueNode>> i = o.getChildren().iterator();
    while (i.hasNext()) {
      traverse(i.next(), inp, sb);
    }
    return sb;
  }
}
