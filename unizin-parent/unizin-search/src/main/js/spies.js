
const spies = []
const BACKUP = Symbol('Spy Backup')
export function spyOn(obj, method, fake = createSpy()) {
    fake[BACKUP] = obj[method]
    obj[method] = fake

    spies.push({ obj, method })

}

export function restoreSpy(obj, method) {
    obj[method] = obj[method][BACKUP]
}


export function createSpy() {
    const spy = function(...args) {
        spy.calls++
        spy.args.push(args)
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
