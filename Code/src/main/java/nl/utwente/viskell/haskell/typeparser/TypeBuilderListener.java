package nl.utwente.viskell.haskell.typeparser;

import nl.utwente.viskell.haskell.type.Type;
import nl.utwente.viskell.haskell.type.TypeClass;
import nl.utwente.viskell.haskell.type.TypeScope;

import java.util.*;

/**
 * ANTLR listener that builds Type instances.
 */
class TypeBuilderListener extends TypeBaseListener {
    /** Temporary storage area for compound types. */
    private final Stack<List<Type>> stack = new Stack<>();

    /** Temporary store for the last type class. */
    private Optional<TypeClass> typeClass = Optional.empty();

    /** The available type classes. */
    private final Map<String, TypeClass> typeClasses;
    
    /** The type scope in which type variables are looked up and built. */
    TypeScope scope = new TypeScope();

    /**
     * Build a TypeBuilderListener.
     * @param typeClasses The available type classes.
     */
    protected TypeBuilderListener(Map<String, TypeClass> typeClasses) {
        this.typeClasses = typeClasses;
        this.enter();
    }

    /** Build a TypeBuilderListener without type class support. */
    protected TypeBuilderListener() {
        this(new HashMap<>());
    }

    @Override
    public void enterTypeWithClass(TypeParser.TypeWithClassContext ctx) {
        this.typeClass = Optional.empty();
    }

    @Override
    public void exitClassedType(TypeParser.ClassedTypeContext ctx) {
        if (this.typeClass.isPresent()) {
            this.scope.introduceConstraint(ctx.getText(), this.typeClass.get());
        }
    }

    @Override
    public void exitTypeClass(TypeParser.TypeClassContext ctx) {
        this.typeClass = Optional.ofNullable(this.typeClasses.get(ctx.getText()));
    }

    @Override
    public final void exitVariableType(TypeParser.VariableTypeContext ctx) {
        this.addParam(this.scope.getVar(ctx.getText()));
    }

    @Override
    public final void enterFunctionType(TypeParser.FunctionTypeContext ctx) {
        this.enter();
    }

    @Override
    public final void exitFunctionType(TypeParser.FunctionTypeContext ctx) {
        Type[] params = this.popParams();
        this.addParam(Type.fun(params[0], params[1])); // We can do this because the grammer makes sure that a function
                                                        // always has two arguments.
    }

    @Override
    public final void enterTupleType(TypeParser.TupleTypeContext ctx) {
        this.enter();
    }

    @Override
    public final void exitTupleType(TypeParser.TupleTypeContext ctx) {
        this.addParam(Type.tupleOf(this.popParams()));
    }

    @Override
    public final void enterListType(TypeParser.ListTypeContext ctx) {
        this.enter();
    }

    @Override
    public final void exitListType(TypeParser.ListTypeContext ctx) {
        this.addParam(Type.listOf((this.popParams()[0])));
    }

    @Override
    public final void exitTypeConstructor(TypeParser.TypeConstructorContext ctx) {
        this.addParam(Type.con(ctx.getText()));
    }

    @Override
    public final void enterConstantType(TypeParser.ConstantTypeContext ctx) {
        this.enter();
    }

    @Override
    public final void exitConstantType(TypeParser.ConstantTypeContext ctx) {
        Type[] types = this.popParams();
        this.addParam(Type.app(types));
    }

    @Override
    public void enterAppliedType(TypeParser.AppliedTypeContext ctx) {
        this.enter();
    }

    @Override
    public void exitAppliedType(TypeParser.AppliedTypeContext ctx) {
        Type[] types = this.popParams();
        this.addParam(Type.app(types));
    }

    /** Call this when entering a compound (function, list, tuple) type. */
    private void enter() {
        this.stack.push(new ArrayList<>());
    }

    /**
     * Call this when adding a part to a compound type.
     *
     * @param t The type to addParam.
     */
    private void addParam(Type t) {
        this.stack.peek().add(t);
    }

    /**
     * Utility method that pops and converts to an array.
     *
     * @return The topmost stack element as an array of Types.
     */
    private Type[] popParams() {
        List<Type> p = this.stack.pop();
        return p.toArray(new Type[p.size()]);
    }

    /**
     * Checks and returns the parse result.
     *
     * @return The result of the parse.
     */
    public Type result() {
        this.assertTrue(this.stack.size() == 1);
        return this.stack.pop().get(0);
    }

    /**
     * Version of assert that also works when Java assertions are off.
     * @param condition This should be true.
     */
    private void assertTrue(boolean condition) {
        if (!condition) {
            throw new RuntimeException("assertTrue failed while treebuilding");
        }
    }
}
