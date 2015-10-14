import React from 'react'
import Cover from '../components/cover.jsx'
import smartLoader from './smart_loader.jsx'
import { ensureDocument } from '../actions/documents.js'
import { routeInsert, routePreviewUrl } from '../actions/route.js'
import Footer from '../components/footer.jsx'
import styles from './result.scss'
import { Link } from 'react-router'
import classNames from 'classnames'
import { joinAuthors } from '../components/search_results.jsx'
import FontAwesome from 'react-fontawesome'
import Date from '../components/date.jsx'
import { checkValue } from '../actions/utils.js'
import { Focus } from 'unizin-js-tools'

const { func, shape, object } = React.PropTypes

class Result extends React.Component {
    static displayName = 'Result'

    static propTypes = {
        document: object.isRequired,
        history: shape({
            goBack: func.isRequired
        }).isRequired,
    }

    constructor(props,context) {
        super(props,context)
        this.state = { expanded: false }
    }

    updateExpanded() {
      this.setState({ expanded: !this.state.expanded })
    }

    render() {
        const { document } = this.props

        const previewUrl = routePreviewUrl(document).url
        const primaryBtnClasses = classNames("btn", "primary", styles.btn)
        const secondaryBtnClasses = classNames("btn", styles.btn)

        // document attributes
        const type = checkValue(document.type)
        const size = checkValue(document.properties["common:size"])
        const language = checkValue(document.properties["hrv:language"])
        const dateAdded = checkValue(document.properties["hrv:date"])
        const rights = checkValue(document.properties["hrv:rights"])
        const description = checkValue(document.properties['hrv:description'])

        return (
            <Focus>
                <main className={styles.result} role="main">
                  <div className={styles.header}>
                    <button onClick={this.props.history.goBack} className={secondaryBtnClasses}>
                        <FontAwesome name='arrow-left' aria-hidden='true' /> Back to results
                    </button>

                    <ul className={styles.controls}>
                      <li>
                        <Link to={routeInsert(document).route} className={primaryBtnClasses} role="button">
                          <FontAwesome name='plus' aria-hidden='true' /> Insert
                        </Link>
                      </li>
                      <li>
                        <a href={previewUrl} target="_blank" className={secondaryBtnClasses} role="button">
                          <FontAwesome name='eye' aria-hidden='true' /> Preview
                        </a>
                      </li>
                    </ul>
                  </div>

                  <div className={styles.wrapper}>
                    <div className={styles.content}>

                      <Cover document={document} className={styles.cover} />

                      <div className={styles.details}>
                        <h1>{document.title}</h1>

                        <div className={styles.author}>
                          Author: {joinAuthors(document.properties['hrv:creator'])}
                        </div>
                        <button onClick={this.updateExpanded}>More</button>

                        <div className={styles.description}>
                          {description}
                        </div>
                      </div>

                    </div>

                    <aside role="complementary">
                      <div className={styles.header}>Additional information</div>
                      <ul className={styles.group}>
                        <li><span>Format</span>{type}</li>
                        <li><span>File size</span>{size}</li>
                        <li><span>Language</span>{language}</li>
                        <li><span>Date</span><Date date={dateAdded} /></li>
                        <li><span>Rights</span>{rights}</li>
                      </ul>
                    </aside>
                  </div>
                  <Footer className={styles.footer} />
                </main>
            </Focus>
        )
    }
}

function mapStateToProps(state, props) {
    const { uid } = props.params

    return {
        document: state.documents[uid]
    }
}

export default smartLoader(
    {
        inputFilter(state, props) {
            return { uid: props.params.uid }
        },
        isReady: ({uid}, state) => (state.documents[uid] != null),
        loader(dispatch, params, lastParams) {
            if (params.uid != lastParams.uid) {
                dispatch(ensureDocument(params.uid))
            }
        }
    },
    mapStateToProps,
    { }
)(Result)
