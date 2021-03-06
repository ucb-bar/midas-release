// See LICENSE for license details.

package strober
package replay

import chisel3.{Module, Data, Element, Bundle, Vec}

private[replay] object getDataNames {
  def apply(name: String, data: Data): Seq[(Element, String)] = data match {
    case e: Element => Seq(e -> name)
    case b: Bundle => b.elements.toSeq flatMap {case (n, e) => apply(s"${name}_$n", e)}
    case v: Vec[_] => v.zipWithIndex flatMap {case (e, i) => apply(s"${name}_$i", e)}
  }
  def apply(dut: Module, separator: String = "."): Seq[(Element, String)] =
    apply(dut.io.pathName replace (".", separator), dut.io)
}

private[replay] object getPorts {
  def apply(dut: Module, separator: String = ".") =
    getDataNames(dut, separator) partition (_._1.dir == chisel3.INPUT)
}

private[replay] object genVerilogFragment {
  // Generate verilog harness fragment
  def apply(dutName: String, io: Data, writer: java.io.Writer) {
    val (inputs, outputs) = getDataNames("io", io) partition (_._1.dir == chisel3.INPUT)

    writer write s"  `define TOP_TYPE $dutName\n"
    inputs foreach { case (node, name) =>
      writer write s"  reg[${node.getWidth-1}:0] $name;\n"
      writer write s"  wire[${node.getWidth-1}:0] #0.01 ${name}_delay = $name;\n"
    }
    outputs foreach { case (node, name) =>
      writer write s"  reg[${node.getWidth-1}:0] $name;\n"
      writer write s"  wire[${node.getWidth-1}:0] ${name}_delay;\n"
    }

    writer write "  always @(posedge clock) begin\n"
    writer write (outputs.unzip._2 map (name => s"    $name <= ${name}_delay;") mkString "\n")
    writer write "\n  end\n"

    writer write "\n  /*** DUT instantiation ***/\n"
    writer write s"  ${dutName} ${dutName}(\n"
    writer write "    .clock(clock),\n"
    writer write "    .reset(reset),\n"
    writer write ((inputs ++ outputs).unzip._2 map (name => s"    .${name}(${name}_delay)") mkString ",\n")
    writer write "\n  );\n\n"

    writer.close
  }
}
