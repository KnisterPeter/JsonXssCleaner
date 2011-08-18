package JsonXssCleaner;

import java.util.List;

public class ValueNode implements IValueNode {
  String str = "";

  public ValueNode(final String s) {
    this.str = s;
  }

  ValueNode setValue(final String _str) {
    this.str = _str;
    return this;
  }

  String getValue() {
    return this.str;
  }

  @Override
  public List<IValueNode> getChildren() {
    // TODO Auto-generated method stub
    return null;
  }
}
