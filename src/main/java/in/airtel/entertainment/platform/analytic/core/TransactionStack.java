package in.airtel.entertainment.platform.analytic.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TransactionStack {

    private final List<TransactionData> stack;

    private TransactionStack(List<TransactionData> stack) {
        this.stack = Collections.unmodifiableList(stack);
    }

    public static TransactionStack empty() {
        return new TransactionStack(Collections.emptyList());
    }

    public TransactionStack push(TransactionData transaction) {
        List<TransactionData> newStack = new ArrayList<>(stack);
        newStack.add(transaction);
        return new TransactionStack(newStack);
    }

    public TransactionStack pop() {
        if (stack.isEmpty()) {
            return this;
        }
        List<TransactionData> newStack = new ArrayList<>(stack.subList(0, stack.size() - 1));
        return new TransactionStack(newStack);
    }

    public TransactionData peek() {
        if (stack.isEmpty()) {
            return null;
        }
        return stack.get(stack.size() - 1);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    public TransactionData getParent() {
        if (stack.size() < 2) {
            return null;
        }
        return stack.get(stack.size() - 2);
    }
}
