start
  = value

value
  = 
  value:(string / number / object / array / "true" / "false" / "null" ) ws
  { return value }

object
  = "{" ws 
    kv:( 
      string:string ws ":" ws value:value 
      next:( ws "," ws string:string ws ":" ws value:value { return "," + string + ":" + value } )*
      { return string + ":" + value + next.join('') }
    )? 
    ws "}"
    { return "{" + kv + "}" }

array
  = "[" ws elements:(
      value:value 
      next:( ws "," ws value:value { return "," + value })*
      { return value + next.join('') }
    )? 
    ws "]"
    { return "[" + elements + "]" }

string
  = '"' 
    chars:(
      hashEntity
      / [^"\\'`<>]
      / ("<" / "\u003C") { return "&lt;" }
      / (">" / "\u003E") { return "&gt;" }
      / ("'" / "\u0027") { return "&apos;" }
      / ("`" / "\u0060") { return "&#96;" }
      / ("\\\"" / "\\\u0022") { return "&quot;" }
      / ("\\" char:('\\' / '/' / 'b' / 'f' / 'n' / 'r' / 't' / 'u' hex hex hex hex) { return '\\' + char })
    )*
    '"'
    { return '"' + chars.join('') + '"' }

hashEntity
  = "&#" ws 
    digits:(
       head:[1-9] tail:[0-9]* { return [10, head + tail.join('')] }
     / [Xx] ws hex:[0-9A-Fa-f]+ { return [16, hex.join('')] }
     / "0" oct:[0-7]+ { return [8, oct.join('')] }
    )
    ws ";"?
    { 
      var num = Number(parseInt(digits[1], digits[0])).toString(16);
      return {
        "3c": "&lt;",
        "3e": "&gt;",
        "27": "&apos;",
        //"60": "&#96;", // intended
        "22": "&quot;"
      }[num]||"&#x" + num + ";";
    }

number
  = sign:"-"?
    pre:([0] / head:[1-9] tail:digit* { return head + tail.join('') })
    post:("." digit:digit+ { return "." + digit.join('') })?
    exp:([eE] sign:[+-]? digits:digit+ { return "e" + sign + digits.join('') })?
    { return sign + pre + post + exp }

hex
  = [0-9A-Fa-f]

digit
  = [0-9]

ws 
  = [ \t\r\n]*
