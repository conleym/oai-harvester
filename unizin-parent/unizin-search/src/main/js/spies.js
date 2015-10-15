
const spies = []
const BACKUP = Symbol('Spy Backup')
export function spyOn(obj, method, fake) {
    const spy = createSpy(fake)
    spy[BACKUP] = obj[method]
    obj[method] = spy

    spies.push({ obj, method })

}

export function restoreSpy(obj, method) {
    obj[method] = obj[method][BACKUP]
}


export function createSpy(fake) {
    const spy = function(...args) {
        spy.calls++
        spy.args.push(args)
        if (fake) {
            return fake.apply(this, args)
        }
    }
    spy.calls = 0
    spy.args = []

    return spy
}

export function restoreAllSpies() {
    while (spies.length > 0) {
        const { obj, method } = spies.shift()
        restoreSpy(obj, method)
    }
}
