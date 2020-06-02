import React from 'react';
import PropTypes from 'prop-types';
import ProjectBoxMiddleComponentDiv from "./ProjectBoxMiddleComponentDiv.jsx";
import ProjectBoxRightComponent from "./ProjectBoxRightComponent.jsx";

class ProjectEntryBox extends React.Component {
    static propTypes = {
        key: PropTypes.number.isRequired,   //standard react disambiguation key
        interfaceSize: PropTypes.number.isRequired,  //0 for large, 1 for small
        projectId: PropTypes.number.isRequired,    //project entry id
        projectTypeId: PropTypes.number.isRequired,
        projectTitle: PropTypes.string.isRequired,
        projectOwner: PropTypes.string.isRequired,
    }

    render(){
        return <div key={this.props.key} className={this.props.interfaceSize===0 ? "project_box_div_version" : "project_box_div_version_small"}>
            <ProjectBoxMiddleComponentDiv id={this.props.projectId}
                                          type={this.props.projectTypeId}
                                          title={this.props.projectTitle}
                                          user={this.props.projectOwner}
                                          size={this.props.interfaceSize} />
            <ProjectBoxRightComponent size={this.props.interfaceSize} />
        </div>
    }
}

export default ProjectEntryBox;