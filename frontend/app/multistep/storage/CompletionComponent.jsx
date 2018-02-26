import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import SummaryComponent from './SummaryComponent.jsx';
import ErrorViewComponent from '../common/ErrorViewComponent.jsx';

class StorageCompletionComponent extends React.Component {
    static propTypes = {
        loginDetails: PropTypes.object.required,
        rootpath: PropTypes.string.required,
        selectedType: PropTypes.number.required,
        clientpath: PropTypes.string.required
    };

    constructor(props){
        super(props);

        this.state = {
            inProgress: false,
            error: null
        };
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
            password: this.props.loginDetails.password
        }
    }

    confirmClicked(event){
        this.setState({inProgress: true});
        axios.request({method: "PUT", url: "/api/storage",data: this.requestContent()}).then(
            (response)=>{
                this.setState({inProgress: false});
                window.location.assign('/storage/');
            }
        ).catch(
            (error)=>{
                this.setState({inProgress: false, error: error});
                console.error(error)
            }
        )
    }

    render() {
        const selectedStorage = this.props.strgTypes[this.props.selectedType];

        const errorLabel = this.state.error ? <span className="error-text">{this.state.error.statusText}: {this.state.error.data.detail}</span> : "";

        return(<div>
            <h3>Set up storage</h3>
            <p className="information">We will set up a new storage definition with the information below.</p>
            <p className="information">Press "Confirm" to go ahead, or press Previous if you need to amend any details.</p>
            <SummaryComponent name={selectedStorage.name} loginDetails={this.props.loginDetails} subfolder={this.props.rootpath} clientpath={this.props.clientpath} />
            <ErrorViewComponent error={this.state.error}/>
            <span style={{float: "right"}}>{errorLabel}<button onClick={this.confirmClicked}>Confirm</button></span>
        </div>)
    }
}

export default StorageCompletionComponent;
