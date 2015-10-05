import React from 'react'
import Cover from '../components/cover.jsx'
import { connect } from 'react-redux'
import { ensureDocument } from '../actions/documents.js'
import { routeInsert, routePreviewUrl } from '../actions/route.js'
import Footer from '../components/footer.jsx'
import styles from './result.scss'
import { Link } from 'react-router'
import classNames from 'classnames'
import { joinAuthors } from '../components/search_results.jsx'
import FontAwesome from 'react-fontawesome'

const { func, func: dispatchFunc, shape, string, any } = React.PropTypes

class Result extends React.Component {
    static displayName = 'Result'

    static propTypes = {
        ensureDocument: dispatchFunc.isRequired,
        params: shape({
            uid: string.isRequired
        }).isRequired,
        document: any,
        history: shape({
            goBack: func.isRequired
        }).isRequired,
    }


    componentDidMount() {
        this.props.ensureDocument(this.props.params.uid)
    }

    render() {
        const { document } = this.props
        if (!document) {
            return null
        }

        const previewUrl = routePreviewUrl(document).url
        const primaryBtnClasses = classNames("btn", "primary", styles.btn)
        const secondaryBtnClasses = classNames("btn", styles.btn)

        return (
            <main className={styles.result} role="main">
              <div className={styles.header}>
                <button onClick={this.props.history.goBack} role="button">
                    <FontAwesome name='arrow-left' /> Back to results
                </button>

                <ul className={styles.controls}>
                  <li>
                    <Link to={routeInsert(document).route} className={primaryBtnClasses} role="button">
                      <FontAwesome name='plus' /> Insert
                    </Link>
                  </li>
                  <li>
                    <a href={previewUrl} target="_blank" className={secondaryBtnClasses} role="button">
                      <FontAwesome name='eye' /> Preview
                    </a>
                  </li>
                </ul>
              </div>

              <div className={styles.wrapper}>
                <div className={styles.content}>

                  <Cover document={document} className={styles.cover} />

                  <div className={styles.details}>
                    <h1>{document.title}</h1>

                    <h2>Author: {joinAuthors(document.properties['hrv:creator'])}</h2>

                    <div className={styles.description}>
                      {document.properties['hrv:description']}
                    </div>
                  </div>

                </div>

                <aside role="complementary">
                  <h1>Additional information</h1>
                  <ul className={styles.group}>
                    <li><span>Format</span>{document.type}</li>
                    <li><span>File size</span>{(document.properties["common:size"])}</li>
                    <li><span>Language</span>{document.properties["hrv:language"]}</li>
                    <li><span>Date added</span>{document.properties["hrv:date"]}</li>
                    <li><span>Rights</span>{document.properties["hrv:rights"]}</li>
                  </ul>
                </aside>
              </div>
              <Footer className={styles.footer} />
            </main>
        )
    }
}

function mapStateToProps(state, props) {
    const { uid } = props.params

    return {
        document: state.documents[uid]
    }
}

export default connect(
  mapStateToProps,
  { ensureDocument }
)(Result)
