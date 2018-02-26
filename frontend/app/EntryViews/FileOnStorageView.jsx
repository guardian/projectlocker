import React from 'react';
import GenericEntryView from './GenericEntryView.jsx';
import ErrorViewComponent from '../multistep/common/ErrorViewComponent.jsx';

import PropTypes from 'prop-types';

class FileOnStorageView extends GenericEntryView {
    static propTypes = {
        entryId: PropTypes.number.isRequired,
        filepath: PropTypes.string.isRequired
    };

    constructor(props){
        super(props);
        this.endpoint = "/api/storage"
    }

    render(){
        if(this.state.lastError) {
            return <ErrorViewComponent error={this.state.lastError}/>
        } else if(this.state.loading) {
            return <span/>
        } else {
            if(this.state.content.clientpath){
                return <a href={"pluto:openproject:" + this.state.content.clientpath + "/" + this.props.filepath}>{this.props.filepath} on {this.state.content.storageType} {this.state.content.clientpath}</a>
            } else {
                return <span>{this.props.filepath} on {this.state.content.storageType} {this.state.content.rootpath}</span>
            }
        }
    }
}

export default FileOnStorageView;
