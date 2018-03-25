package jmc.cas.operations;

import jmc.cas.*;
import jmc.cas.components.*;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import static jmc.cas.operations.Argument.*;

/**
 * Created by Jiachen on 3/17/18.
 * Custom Operation
 */
public class CustomOperation extends Operation implements BinLeafNode, Nameable {
    private static ArrayList<Manipulation> manipulations = new ArrayList<>();
    private Manipulation manipulation;

    static {
        define(Calculus.SUM, Signature.ANY, (operands -> operands.stream().reduce(Operable::add).get()));
        define(Calculus.DERIVATIVE, new Signature(ANY, VARIABLE), (operands -> operands.get(0).firstDerivative((Variable) operands.get(1))));
        define(Calculus.DERIVATIVE, new Signature(ANY, VARIABLE, NUMBER), (operands -> {
            double i = operands.get(2).val();
            if (!RawValue.isInteger(i)) throw new JMCException("order of derivative must be an integer");
            return operands.get(0).derivative((Variable) operands.get(1), (int) i);
        }));
        define("simplify", new Signature(ANY), operands -> operands.get(0).simplify());
        define("simplest", new Signature(ANY), operands -> operands.get(0).simplest());
        define("expand", new Signature(ANY), operands -> operands.get(0).expand());
        define("num_nodes", new Signature(ANY), operands -> new RawValue(operands.get(0).numNodes()));
        define("complexity", new Signature(ANY), operands -> new RawValue(operands.get(0).complexity()));
        define("replace", new Signature(ANY, ANY, ANY), operands -> operands.get(0).replace(operands.get(1), operands.get(2)));
        define("beautify", new Signature(ANY), operands -> operands.get(0).beautify());
        define("val", new Signature(ANY), operands -> new RawValue(operands.get(0).val()));
        define("eval", new Signature(ANY, NUMBER), operands -> new RawValue(operands.get(0).eval(operands.get(1).val())));

        define("define", new Signature(LITERAL, NUMBER), operands -> {
            String constant = ((Literal) operands.get(0)).get();
            Constants.define(constant, () -> operands.get(1).val());
            return Constants.get(constant);
        });

        define("define", new Signature(LITERAL, LIST, ANY), operands -> {
            String name = ((Literal) operands.get(0)).get();
            List arguments = ((List) operands.get(1));
            final Operable operation = operands.get(2);
            Signature signature = new Signature(arguments.size());

            // unregister existing manipulations with the same signature.
            ArrayList<Manipulation> overridden = unregister(name, signature);
            String s = "Overridden: " + toString(overridden);
            Literal msg = new Literal(overridden.size() == 0 ? "Done." : s);
            define(name, signature, feed -> {
                ArrayList<Operable> args = arguments.unwrap();
                Operable tmp = operation.copy();
                for (int i = 0; i < args.size(); i++) {
                    Operable arg = args.get(i);
                    tmp = tmp.replace(arg, feed.get(i));
                }
                return tmp.simplify();
            });
            return msg;
        });

        define("remove", Signature.ANY, operands -> {
            ArrayList<Manipulation> removed = new ArrayList<>();
            operands.forEach(o -> {
                if (!(o instanceof Literal)) throw new JMCException("illegal argument " + o);
                removed.addAll(unregister(((Literal) o).get(), Signature.ANY));
            });
            return new Literal("Removed: " + toString(removed));
        });
    }

    private static String toString(ArrayList<Manipulation> manipulations) {
        return "[" + manipulations.stream()
                .map(Manipulation::toString)
                .reduce((a, b) -> a + ", " + b)
                .orElse("") + "]";
    }


    public static ArrayList<Manipulation> unregister(String name, Signature signature) {
        ArrayList<Manipulation> unregistered = new ArrayList<>();
        for (int i = manipulations.size() - 1; i >= 0; i--) {
            Manipulation manipulation = manipulations.get(i);
            if (manipulation.equals(name, signature))
                unregistered.add(manipulations.remove(i));
        }
        return unregistered;
    }


    public CustomOperation(String name, Operable... operands) {
        this(name, wrap(operands));
    }

    public CustomOperation(String name, ArrayList<Operable> operands) {
        super(operands);
        this.manipulation = resolveManipulation(name, Signature.resolve(operands));
    }

    private static Manipulation resolveManipulation(String name, Signature signature) {
        ArrayList<Manipulation> candidates = manipulations.stream()
                .filter(o -> o.getName().equals(name))
                .collect(Collectors.toCollection(ArrayList::new));
        for (Manipulation manipulation : candidates) { //prioritize explicit signatures
            if (manipulation.getSignature().equals(signature)) {
                return manipulation;
            }
        }
        for (Manipulation manipulation : candidates) {
            if (manipulation.getSignature().equals(Signature.ANY)) {
                return manipulation;
            }
        }
        throw new JMCException("cannot resolve operation \"" + name + "\" with signature " + signature);
    }

    public static ArrayList<Manipulation> registeredManipulations() {
        return manipulations;
    }

    public static void define(String name, Signature signature, Manipulable manipulable) {
        manipulations.add(new Manipulation(name, signature, manipulable));
    }

    public static void register(Manipulation manipulation) {
        manipulations.add(manipulation);
    }


    public String toString() {
        Optional<String> args = getOperands().stream()
                .map(Operable::toString)
                .reduce((a, b) -> a + "," + b);
        return getName() + "(" + (args.orElse("")) + ")";
    }

    public double val() {
        return manipulation.manipulate(getOperands()).val();
    }

    public String getName() {
        return manipulation.getName();
    }

    public double eval(double x) {
        return manipulation.manipulate(getOperands()).eval(x);
    }

    @Override
    public Operable simplify() {
        super.simplify();
        return manipulation.manipulate(getOperands()).simplify(); //TODO: might cause StackOverflow
    }

    public Operable exec() {
        return manipulation.manipulate(getOperands());
    }

    @Override
    public Operable firstDerivative(Variable v) {
        return this.simplify().firstDerivative(v);
    }

    public CustomOperation copy() {
        return new CustomOperation(getName(), getOperands().stream()
                .map(Operable::copy)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    public boolean equals(Operable o) {
        if (!super.equals(o)) return false;
        if (o instanceof CustomOperation) {
            CustomOperation co = ((CustomOperation) o);
            return co.getName().equals(getName());
        }
        return false;
    }
}