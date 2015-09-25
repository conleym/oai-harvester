import styles from './pager.scss'
import React from 'react'

const { number } = React.PropTypes

export default class Pager extends React.Component {
    static displayName = 'Pager'

    static propTypes = {
        current: number,
        max: number
    }

    constructor(props, context) {
        super(props, context)

        this.onClick = this.onClick.bind(this)
    }

    onClick(e) {
        e.preventDefault()
        const page = parseInt(e.target.value, 10)
        this.props.onChange(page)
    }

    render() {
        const { max, current } = this.props
        const buttons = []
        const pager_name = this.props.ariaLabel

        if (this.props.ariaLabel == null) {
            console.warn( // eslint-disable-line no-console
              "It looks like you did not provide an 'ariaLabel' when calling the Pager component."
            )
        }

        let from = current - 2
        let to = current + 2

        if (from <= 1) {
            from = 1
            to = 5
        }
        if (to >= max) {
            to = max
            from = to - 5
        }

        for (let i = from; i <= to; i++) {
            buttons.push({
                label: i,
                i
            })
        }

        if (from > 1) {
            buttons.unshift({
                label: 1,
                i: 1,
            })
        }

        if (current > 1) {
            buttons.unshift({
                label: 'Previous',
                i: current - 1
            })
        }

        if (to < max) {
            buttons.push({
                label: max,
                i: max,
            })
        }

        if (current < max) {
            buttons.push({
                label: 'Next',
                i: current + 1
            })
        }

        return (
          <nav role="navigation" aria-label={pager_name}>
            <ul className={styles.pagination}>
              {buttons.map(({label, i}) => (
                  <li key={label}>
                    <button
                      value={i}
                      onClick={this.onClick}
                      disabled={i == current}
                      aria-disabled={i == current}
                      aria-role="button"
                      className={i == current ? styles.active : undefined}>
                      {label}
                    </button>
                  </li>
              ))}
            </ul>
          </nav>
        )
    }
}
