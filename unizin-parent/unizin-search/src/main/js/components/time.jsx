import React from 'react'

const { string } = React.PropTypes

export default class Time extends React.Component {

  static displayName = 'Time'

  static propTypes = {
      formattedDate: string.isRequired,
      dateTime: string.isRequired
  }

  constructor(props, context) {
      super(props, context)

      this.formattedDate = this.props.formattedDate
      this.dateTime = this.props.dateTime
  }

  render() {
      return(
          <time dateTime={this.dateTime}>
              {this.formattedDate}
          </time>
    )
  }
}
