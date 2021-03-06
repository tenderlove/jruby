package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.RubyArray;
import org.jruby.ir.IRScope;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReqdArgMultipleAsgnInstr extends MultipleAsgnBase {
    private final int preArgsCount;    // # of reqd args before rest-arg (-1 if we are fetching a pre-arg)
    private final int postArgsCount;   // # of reqd args after rest-arg  (-1 if we are fetching a pre-arg)

    public ReqdArgMultipleAsgnInstr(Variable result, Operand array, int preArgsCount, int postArgsCount, int index) {
        super(Operation.MASGN_REQD, result, array, index);
        this.preArgsCount = preArgsCount;
        this.postArgsCount = postArgsCount;
    }

    public ReqdArgMultipleAsgnInstr(Variable result, Operand array, int index) {
        this(result, array, -1, -1, index);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + array + ", " + index + ", " + preArgsCount + ", " + postArgsCount + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), array.cloneForInlining(ii), preArgsCount, postArgsCount, index);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // ENEBO: Can I assume since IR figured this is an internal array it will be RubyArray like this?
        RubyArray rubyArray = (RubyArray) array.retrieve(context, self, currDynScope, temp);
        Object val;
        
        int n = rubyArray.getLength();
        if (preArgsCount == -1) {
            // Masgn for 1.8 and 1.9 pre-reqd. args always comes down this path!
            return rubyArray.entry(index);
        } else {
            // Masgn for 1.9 post-reqd args always come down this path
            int remaining = n - preArgsCount;
            if (remaining <= index) {
                return context.nil;
            } else {
                return (remaining > postArgsCount) ? rubyArray.entry(n - postArgsCount + index) : rubyArray.entry(preArgsCount + index);
            }
        }
    }
}
