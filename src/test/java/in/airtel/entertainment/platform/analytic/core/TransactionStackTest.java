package in.airtel.entertainment.platform.analytic.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStackTest {

    @Test
    void emptyShouldReturnEmptyStack() {
        TransactionStack stack = TransactionStack.empty();
        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
        assertNull(stack.peek());
    }

    @Test
    void pushShouldAddTransaction() {
        TransactionStack stack = TransactionStack.empty();
        TransactionData td = new TransactionData("tx1");
        TransactionStack pushed = stack.push(td);

        assertFalse(pushed.isEmpty());
        assertEquals(1, pushed.size());
        assertEquals("tx1", pushed.peek().getTransactionName());
        // Original should be unchanged (immutable)
        assertTrue(stack.isEmpty());
    }

    @Test
    void popShouldRemoveTopTransaction() {
        TransactionStack stack = TransactionStack.empty()
                .push(new TransactionData("tx1"))
                .push(new TransactionData("tx2"));

        assertEquals(2, stack.size());
        assertEquals("tx2", stack.peek().getTransactionName());

        TransactionStack popped = stack.pop();
        assertEquals(1, popped.size());
        assertEquals("tx1", popped.peek().getTransactionName());
    }

    @Test
    void popOnEmptyStackShouldReturnSameStack() {
        TransactionStack stack = TransactionStack.empty();
        TransactionStack popped = stack.pop();
        assertTrue(popped.isEmpty());
    }

    @Test
    void getParentShouldReturnSecondFromTop() {
        TransactionStack stack = TransactionStack.empty()
                .push(new TransactionData("parent"))
                .push(new TransactionData("child"));

        assertEquals("parent", stack.getParent().getTransactionName());
    }

    @Test
    void getParentShouldReturnNullWhenSingleElement() {
        TransactionStack stack = TransactionStack.empty()
                .push(new TransactionData("only"));

        assertNull(stack.getParent());
    }
}
