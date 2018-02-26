import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';

class ProjectTemplateDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Project";
        this.endpoint = "/api/project";
    }

    // informationPara(){
    //     if(this.state.warning)
    //         return <p className="warning">{this.state.warning}</p>;
    //     else
    //         return <p className="information">The following {this.itemClass} will be PERMANENTLY deleted, along with the file on-disk,
    //             if you click the Delete button below.  Do you want to continue?</p>;
    // }
}

export default ProjectTemplateDeleteComponent;