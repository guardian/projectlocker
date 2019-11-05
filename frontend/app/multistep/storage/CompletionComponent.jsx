import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';
import CommonCompletionComponent from '../common/CommonCompletionComponent.jsx';

class StorageCompletionComponent extends CommonCompletionComponent {
    static propTypes = {
        loginDetails: PropTypes.object.required,
        rootpath: PropTypes.string.required,
        selectedType: PropTypes.number.required,
        clientpath: PropTypes.string.required,
        enableVersions: PropTypes.bool.isRequired,
        nickname: PropTypes.string.isRequired,
        nicknameChanged: PropTypes.string.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };

        this.endpoint = "/api/storage";
        this.successRedirect = "/storage/";
        this.confirmClicked = this.confirmClicked.bind(this);
    }

    requestContent(){
        const selectedStorage = this.props.strgTypes[this.props.selectedType];
        return {
            rootpath: this.props.rootpath,
            clientpath: this.props.clientpath,
            storageType: selectedStorage.name,
            host: this.props.loginDetails.hostname,
            port: this.props.loginDetails.port ? parseInt(this.props.loginDetails.port) : null,
            user: this.props.loginDetails.username,
            password: this.props.loginDetails.password,
            device: this.props.loginDetails.device,
            supportsVersions: this.props.enableVersions,
            nickname: this.props.nickname
        }
    }

    render() {
        const selectedStorage = this.props.strgTypes[this.props.selectedType];
        return(<div>
            <h3>Set up storage</h3>
            <p className="information">We will set up a new storage definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent name={selectedStorage.name}
                              loginDetails={this.props.loginDetails}
                              subfolder={this.props.rootpath}
                              clientpath={this.props.clientpath}
                              enableVersions={this.props.enableVersions}
                              nicknameChanged={this.props.nicknameChanged}
                              nickname={this.props.nickname}
            />
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}><button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default StorageCompletionComponent;
