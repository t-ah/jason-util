package jason.arch.helpers;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

    private final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

    @Override
    public void publish(LogRecord record) {
        var msg = record.getMessage();

        var type = Failure.getErrorType(msg);
        if (msg.startsWith("No failure")  || msg.startsWith("Found")) {
            messages.add(msg);
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}

    /**
     * @return the first message in the queue or null if there is no message.
     */
    public String checkMessage() {
        return this.messages.poll();
    }
}
