package org.kaazing.nuklei.amqp_1_0.codec.definitions;

import org.kaazing.nuklei.FlyweightBE;
import org.kaazing.nuklei.amqp_1_0.codec.types.Type;
import uk.co.real_logic.agrona.DirectBuffer;

import java.util.function.Consumer;

/**
 * Created by Jesse on 3/24/2015.
 */
public class Optional<T extends Type>
{
    private Consumer<Optional<T>> observer = (owner) -> {};  // NOP
    protected final Nullable nullable;
    protected T type;

    public Optional(T type)
    {
        super();
        nullable = new Nullable();
        this.type = type;
    }

    public Optional<T> wrap(DirectBuffer buffer, int offset, boolean mutable)
    {
        type.wrap(buffer, offset, mutable);
        nullable.wrap(buffer, offset, mutable);
        return this;
    }

    public T getType()
    {
        return type;
    }

    public boolean isNull()
    {
        return nullable.get();
    }

    public Optional setNull()
    {
        nullable.set();
        notifyChanged();
        return this;
    }

    public int limit()
    {
        return isNull() ? nullable.limit() : type.limit();
    }


    public Optional<T> watch(Consumer<Optional<T>> observer)
    {
        this.observer = observer;
        return this;
    }

    public final void notifyChanged()
    {
        observer.accept(this);
    }

    protected static final class Nullable extends FlyweightBE
    {

        public boolean get()
        {
            return uint8Get(buffer(), offset()) == 0x40;
        }

        public void set()
        {
            uint8Put(mutableBuffer(), offset(), (short) 0x40);
        }

        @Override
        public int limit()
        {
            return offset() + 1;
        }

        @Override
        public Nullable wrap(DirectBuffer buffer, int offset, boolean mutable)
        {
            super.wrap(buffer, offset, mutable);
            return this;
        }
    }
}
