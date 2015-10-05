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
      // in some cases we won't get a date back.
      // for examplethere might be a string representation for the dc.date.avaiable
      // http://deepblue.lib.umich.edu/handle/2027.42/55391?show=full
      // in that case we'll just return the string instead of the formatted date

      if (moment(date).isValid()) {
          return moment(date).format('MMMM D, YYYY')
      } else {
          return date
      }
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
                {dates.map((date, index) => {
                    return (
                      <li key={'date' + index}>
                          <Time formattedDate={this.formatString(date)} dateTime={date} />
                      </li>
                    )
                })}
            </ul>
          )
      }
  }
}
