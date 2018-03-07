import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/postrun/SummaryComponent.jsx';

class PostrunDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Postrun action";
        this.endpoint = "/api/postrun";
    }

    getSummary(){
        return <SummaryComponent title={this.state.selectedItem.title} description={this.state.selectedItem.description}/>;
    }

}

export default PostrunDeleteComponent;