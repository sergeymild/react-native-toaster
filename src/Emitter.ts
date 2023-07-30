interface EventsMap {
  [event: string]: any;
}

interface DefaultEvents extends EventsMap {
  [event: string]: (...args: any) => void;
}

interface Unsubscribe {
  (): void;
}

export class Emitter<Events extends EventsMap = DefaultEvents> {
  events: Partial<{ [E in keyof Events]: Events[E][] }> = {};

  emit<K extends keyof Events>(
    this: this,
    event: K,
    ...args: Parameters<Events[K]>
  ) {
    let callbacks = this.events[event] ?? [];
    for (let i = 0, length = callbacks.length; i < length; i++) {
      callbacks[i]!(...args);
    }
  }

  on<K extends keyof Events>(this: this, event: K, cb: Events[K]): Unsubscribe {
    this.events[event]?.push(cb) || (this.events[event] = [cb]);
    return () => {
      this.events[event] = this.events[event]?.filter((i) => cb !== i);
    };
  }
}
