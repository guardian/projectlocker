import React from 'react';
import axios from 'axios';
import SortableTable from 'react-sortable-table';
import {Link} from 'react-router-dom';
import moment from 'moment';
import PropTypes from 'prop-types';

class ProjectBoxMiddleComponent extends React.Component {
    static propTypes = {
        location: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);

    }

    render() {
        return (
            <div className="middle_project_box">
                <table>
                    <tr>
                        <td>

                        </td>
                        <td>
                            <img src="/assets/images/project.png"/>
                        </td>
                        <td className="middle_project_box_data">
                            <div className="middle_project_box_text">
                                {this.props.title}
                            </div>
                        </td>
                        <td>
                            <a href="">KP-12345</a>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/>
                        </td>
                        <td>
                            <img src="/assets/images/commission.png"/>
                        </td>
                        <td className="middle_project_box_data">
                            Place Holder Commission
                        </td>
                        <td>
                            <a href="">KP-54321</a>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/>
                        </td>
                        <td>
                            <i className="fa fa-users" style={{marginLeft: "0.5em", color: "green"}}/>
                        </td>
                        <td className="middle_project_box_data">
                            Place Holder Working Group
                        </td>
                        <td>

                        </td>
                    </tr>
                    <tr>
                        <td>
                            <i className="fa fa-search-plus" style={{marginLeft: "0.5em"}}/>
                        </td>
                        <td>
                            <i className="fa fa-user" style={{marginLeft: "0.5em"}}/>
                        </td>
                        <td className="middle_project_box_data">
                            {this.props.user}
                        </td>
                        <td>

                        </td>
                    </tr>
                </table>
            </div>
        );
    }
}

export default ProjectBoxMiddleComponent;