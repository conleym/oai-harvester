import React from 'react'
import moment from 'moment'
import Time from './time.jsx'

const { any } = React.PropTypes

export default class Date extends React.Component {

  static displayName = 'Date'

  static propTypes = {
      date: any.isRequired,
  }

  formatString(date) {
      return moment(date).format('MMMM D, YYYY')
  }

  render() {
      // dates will come back as either a string or an array
      if (typeof this.props.date === 'string') {
          return <Time formattedDate={this.formatString(this.props.date)} dateTime={this.props.date} />
      } else {
          // iterate over each date and render the <Time /> component
          const dates = this.props.date
          return (
            <ul>
                {dates.map((date) => {
                    console.log(date)
                    return <li><Time formattedDate={this.formatString(date)} dateTime={date} /></li>
                })}
            </ul>
          )
      }
  }
}
