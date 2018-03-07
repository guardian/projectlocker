import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import CommonCompletionComponent from '../common/CommonCompletionComponent.jsx';

class CompletionComponent extends CommonCompletionComponent {
    static propTypes = {
        postrunMetadata: PropTypes.object.isRequired,
        postrunSource: PropTypes.string.isRequired,
        currentEntry: PropTypes.number.isRequired
    };

    constructor(props){
        super(props);
        this.state = {
            loading: false,
            loadingError: null
        };
        this.endpoint = "/api/postrun";
        this.successRedirect = "/postrun/";
    }

    requestContent(){
        return this.props.postrunMetadata;
    }

    render(){
        return <div>
            <h3>Edit postrun action metadata</h3>
            <p className="information">The postrun action information will be edited as below. Click Confirm to continue or Back to change.</p>
            <SummaryComponent title={this.props.postrunMetadata.title} description={this.props.postrunMetadata.description}/>
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>
    }
}

export default CompletionComponent;
